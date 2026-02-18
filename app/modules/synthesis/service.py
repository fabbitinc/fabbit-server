"""합성 도메인 서비스 레이어."""

import time
import uuid
from datetime import datetime, timezone

import pandas as pd
from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.background_worker import guarded
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.excel_parser import get_sheet_names, read_to_dataframe
from app.infrastructure.s3_client import S3Client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.cypher_utils import format_cypher_value
from app.modules.ontology.schemas import MappingResult, RelationMapping
from app.modules.part import repository as part_repo
from app.modules.synthesis import repository as repo
from app.modules.synthesis.models import SynthesisJob
from app.modules.synthesis.schemas import (
    SynthesisBatchFailure,
    SynthesisBatchItemStatus,
    SynthesisBatchStartRequest,
    SynthesisBatchStartResponse,
    SynthesisBatchStatusResponse,
    SynthesisJobResponse,
    SynthesisListResponse,
    SynthesisStartRequest,
)

_s3 = S3Client()

CHUNK_SIZE = 500


def _has_rootless_consists_of(mapping_dict: dict) -> bool:
    """매핑 dict에 node_columns가 비어있는 CONSISTS_OF가 있는지 확인."""
    for rm in mapping_dict.get("relation_mappings", []):
        if rm.get("rel_type") == "CONSISTS_OF" and not rm.get("node_columns"):
            return True
    return False


@transactional
def start_synthesis(
    db: Session,
    auth: AuthContext,
    req: SynthesisStartRequest,
    add_background_task,
) -> SynthesisJobResponse:
    if req.mapping_id is not None:
        record = repo.get_mapping_by_id(db, req.mapping_id)
        if record is None:
            raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    else:
        record = repo.get_latest_mapping(db)
        if record is None:
            raise AppError(
                message="조직에 등록된 매핑이 없습니다. 먼저 매핑을 확정해주세요.",
                code="NOT_FOUND",
            )

    # Root-Specified BOM 검증
    if _has_rootless_consists_of(record.mapping) and not req.root_part_number:
        raise AppError(
            message="이 매핑은 Root-Specified BOM입니다. root_part_number를 지정해주세요.",
            code="INVALID_INPUT",
        )

    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")
    if upload.status != "UPLOADED":
        raise AppError(
            message="업로드가 완료되지 않은 파일입니다. 먼저 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    job = repo.create_synthesis_job(
        db=db,
        job_id=generate_uuid7(),
        mapping_id=record.id,
        upload_id=upload.id,
    )
    repo.increment_mapping_usage(db, record)
    db.flush()
    db.refresh(job)

    schema_name = org_id_to_schema(auth.org_id)
    add_background_task(
        guarded(_run_synthesis),
        job_id=job.id,
        schema_name=schema_name,
        graph_name=schema_name,
        file_key=upload.file_key,
        filename=upload.original_name,
        sheet_name=record.sheet_name,
        mapping_json=record.mapping,
        root_part_number=req.root_part_number,
    )

    logger.info(
        "합성 작업 시작: job_id={job_id} mapping_id={mapping_id} upload_id={upload_id}",
        job_id=job.id,
        mapping_id=record.id,
        upload_id=upload.id,
    )
    return _to_job_response(job)


@transactional
def start_synthesis_batch(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    req: SynthesisBatchStartRequest,
    add_background_task,
) -> SynthesisBatchStartResponse:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    if req.mapping_id is not None:
        record = repo.get_mapping_by_id(db, req.mapping_id)
        if record is None:
            raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    else:
        record = repo.get_latest_mapping_by_project(db, project_id)
        if record is None:
            record = repo.get_latest_mapping(db)
        if record is None:
            raise AppError(
                message="조직에 등록된 매핑이 없습니다. 먼저 매핑을 확정해주세요.",
                code="NOT_FOUND",
            )

    # Root-Specified BOM 검증
    if _has_rootless_consists_of(record.mapping) and not req.root_part_number:
        raise AppError(
            message="이 매핑은 Root-Specified BOM입니다. root_part_number를 지정해주세요.",
            code="INVALID_INPUT",
        )

    schema_name = org_id_to_schema(auth.org_id)
    accepted_jobs = []
    failed = []
    upload_ids = list(dict.fromkeys(req.upload_ids))

    for upload_id in upload_ids:
        upload = repo.get_upload_by_id(db, upload_id)
        if upload is None:
            failed.append(
                SynthesisBatchFailure(
                    upload_id=upload_id,
                    reason="업로드를 찾을 수 없습니다",
                )
            )
            continue

        if upload.status != "UPLOADED":
            failed.append(
                SynthesisBatchFailure(
                    upload_id=upload_id,
                    reason="업로드가 완료되지 않은 파일입니다",
                )
            )
            continue

        if upload.owner_type != "project" or upload.owner_id != project_id:
            failed.append(
                SynthesisBatchFailure(
                    upload_id=upload_id,
                    reason="해당 프로젝트에 속하지 않은 업로드입니다",
                )
            )
            continue

        job = repo.create_synthesis_job(
            db=db,
            job_id=generate_uuid7(),
            batch_id=None,
            mapping_id=record.id,
            upload_id=upload.id,
        )
        accepted_jobs.append((job, upload))

    batch = repo.create_synthesis_batch(
        db=db,
        batch_id=generate_uuid7(),
        project_id=project_id,
        mapping_id=record.id,
        requested_count=len(req.upload_ids),
        accepted_count=len(accepted_jobs),
        failed_uploads=[item.model_dump(mode="json") for item in failed],
    )

    for job, _upload in accepted_jobs:
        job.batch_id = batch.id

    if accepted_jobs:
        repo.increment_mapping_usage(db, record, len(accepted_jobs))

    db.flush()
    db.refresh(batch)
    for job, _upload in accepted_jobs:
        db.refresh(job)

    for job, upload in accepted_jobs:
        add_background_task(
            guarded(_run_synthesis),
            job_id=job.id,
            schema_name=schema_name,
            graph_name=schema_name,
            file_key=upload.file_key,
            filename=upload.original_name,
            sheet_name=record.sheet_name,
            mapping_json=record.mapping,
            root_part_number=req.root_part_number,
        )

    logger.info(
        "합성 배치 시작: batch_id={batch_id} project_id={project_id} mapping_id={mapping_id} accepted={accepted} failed={failed}",
        batch_id=batch.id,
        project_id=project_id,
        mapping_id=record.id,
        accepted=len(accepted_jobs),
        failed=len(failed),
    )
    return SynthesisBatchStartResponse(
        batch_id=batch.id,
        requested_count=batch.requested_count,
        accepted_count=batch.accepted_count,
        items=[_to_job_response(job) for job, _upload in accepted_jobs],
        failed=failed,
    )


@transactional(read_only=True)
def get_synthesis_job(db: Session, job_id: uuid.UUID) -> SynthesisJobResponse:
    job = repo.get_synthesis_job_by_id(db, job_id)
    if job is None:
        raise AppError(message="합성 작업을 찾을 수 없습니다", code="NOT_FOUND")
    return _to_job_response(job)


@transactional(read_only=True)
def list_synthesis_jobs(db: Session) -> SynthesisListResponse:
    jobs = repo.list_synthesis_jobs(db)
    return SynthesisListResponse(items=[_to_job_response(j) for j in jobs])


@transactional(read_only=True)
def get_synthesis_batch(
    db: Session,
    batch_id: uuid.UUID,
) -> SynthesisBatchStatusResponse:
    batch = repo.get_synthesis_batch_by_id(db, batch_id)
    if batch is None:
        raise AppError(message="합성 배치를 찾을 수 없습니다", code="NOT_FOUND")

    jobs = repo.list_synthesis_jobs_by_batch_id(db, batch_id)
    pending_count = 0
    processing_count = 0
    completed_count = 0
    failed_job_count = 0
    items = []

    for job in jobs:
        if job.status == "PENDING":
            pending_count += 1
        elif job.status == "PROCESSING":
            processing_count += 1
        elif job.status == "FAILED":
            failed_job_count += 1
        elif job.status == "COMPLETED":
            completed_count += 1

        items.append(
            SynthesisBatchItemStatus(
                job_id=job.id,
                upload_id=job.upload_id,
                status=job.status,
                total_rows=job.total_rows,
                processed_rows=job.processed_rows,
                nodes_created=job.nodes_created,
                relationships_created=job.relationships_created,
                error_count=len(job.errors or []),
                started_at=job.started_at,
                completed_at=job.completed_at,
            )
        )

    failed = [
        SynthesisBatchFailure.model_validate(item)
        for item in batch.failed_uploads or []
    ]
    failed_count = len(failed)
    accepted_count = batch.accepted_count
    done_count = completed_count + failed_job_count

    if accepted_count == 0:
        status = "FAILED" if failed_count > 0 else "PENDING"
    elif done_count == accepted_count:
        status = "COMPLETED" if failed_job_count == 0 else "COMPLETED_WITH_ERRORS"
    elif processing_count > 0:
        status = "PROCESSING"
    else:
        status = "PENDING"

    return SynthesisBatchStatusResponse(
        batch_id=batch.id,
        requested_count=batch.requested_count,
        accepted_count=accepted_count,
        failed_count=failed_count,
        pending_count=pending_count,
        processing_count=processing_count,
        completed_count=completed_count,
        failed_job_count=failed_job_count,
        status=status,
        failed=failed,
        items=items,
        created_at=batch.created_at,
    )


# ── Cypher 빌더 ──


def _build_merge_node(
    label: str,
    merge_keys: dict[str, str],
    set_props: dict[str, str],
) -> str:
    merge_str = ", ".join(f"{k}: {v}" for k, v in merge_keys.items())
    # Part는 RDS가 SoT이므로 Graph에 속성 저장하지 않음 (merge key만)
    if label == "Part":
        return f"MERGE (n:{label} {{{merge_str}}})"
    if set_props:
        set_parts = [f"n.{k} = {v}" for k, v in set_props.items()]
        set_str = " SET " + ", ".join(set_parts)
    else:
        set_str = ""
    return f"MERGE (n:{label} {{{merge_str}}}){set_str}"


def _build_merge_rel(
    from_label: str,
    from_keys: dict[str, str],
    to_label: str,
    to_keys: dict[str, str],
    rel_type: str,
    rel_props: dict[str, str],
) -> str:
    from_str = ", ".join(f"{k}: {v}" for k, v in from_keys.items())
    to_str = ", ".join(f"{k}: {v}" for k, v in to_keys.items())
    if rel_props:
        prop_parts = [f"{k}: {v}" for k, v in rel_props.items()]
        rel_prop_str = " {" + ", ".join(prop_parts) + "}"
    else:
        rel_prop_str = ""
    return (
        f"MATCH (a:{from_label} {{{from_str}}}), (b:{to_label} {{{to_str}}}) "
        f"MERGE (a)-[:{rel_type}{rel_prop_str}]->(b)"
    )


# ── 데이터 추출 ──


def _extract_row_part(row: dict, mapping: MappingResult) -> tuple[str | None, dict]:
    """행에서 주인공 Part 속성 추출 (property_mappings 기반).

    Returns:
        (part_number, {속성dict}) — part_number가 없으면 (None, {})
    """
    props: dict = {}
    for pm in mapping.property_mappings:
        val = _cast_python_value(row.get(pm.source_column), pm.data_type)
        if val is not None:
            props[pm.target_property] = val
    pn = props.get("part_number")
    return (str(pn), props) if pn else (None, {})


def _extract_related_parts(row: dict, mapping: MappingResult) -> dict[str, dict]:
    """CONSISTS_OF 관계에서 상대방(상위) Part 속성 추출.

    Returns:
        {part_number: {속성dict}} — 상위 Part가 없으면 빈 dict
    """
    result: dict[str, dict] = {}
    for rm in mapping.relation_mappings:
        if rm.rel_type != "CONSISTS_OF" or rm.target_label != "Part":
            continue
        props: dict = {}
        for prop_name, src_col in rm.node_columns.items():
            val = _cast_python_value(row.get(src_col), "string")
            if val is not None:
                props[prop_name] = val
        pn = props.get("part_number")
        if pn:
            result[str(pn)] = props
    return result


def _merge_part_props(part_data: dict[str, dict], pn: str, props: dict) -> None:
    """Part 속성을 first-non-null 방식으로 병합.

    동일 part_number에 대해 먼저 수집된 값을 유지하고,
    아직 없는 속성만 추가합니다.
    """
    if pn not in part_data:
        part_data[pn] = dict(props)
        return
    existing = part_data[pn]
    for key, value in props.items():
        if key not in existing:
            existing[key] = value


def _extract_bom_data(
    row: dict, mapping: MappingResult, child_pn: str | None
) -> list[dict]:
    """행에서 CONSISTS_OF 관계 데이터 추출.

    Returns:
        [{"parent_pn": "ASM-001", "child_pn": "BRK-001", "quantity": 2, ...}]
    """
    if not child_pn:
        return []
    entries: list[dict] = []
    for rm in mapping.relation_mappings:
        if rm.rel_type != "CONSISTS_OF":
            continue
        # 상위 Part의 part_number (node_columns에서)
        parent_src = rm.node_columns.get("part_number")
        if not parent_src:
            continue
        parent_pn = _cast_python_value(row.get(parent_src), "string")
        if not parent_pn:
            continue

        entry: dict = {"parent_pn": str(parent_pn), "child_pn": child_pn}
        # 관계 속성 (quantity 등)
        for rel_prop, src_col in rm.rel_columns.items():
            data_type = rm.rel_column_types.get(rel_prop, "string")
            val = _cast_python_value(row.get(src_col), data_type)
            if val is not None:
                entry[rel_prop] = val
        entries.append(entry)
    return entries


def _process_row_nodes(row: dict, mapping: MappingResult) -> list[str]:
    """행에서 비-Part 노드 MERGE Cypher 생성.

    Part 노드는 RDS에서 처리하므로 여기서는 Supplier, Drawing 등만 처리합니다.
    """
    cyphers: list[str] = []
    seen: set[tuple] = set()

    for rm in mapping.relation_mappings:
        if rm.target_label == "Part":
            continue

        node_def = MANUFACTURING_ONTOLOGY.get_node_label(rm.target_label)
        if not node_def:
            continue

        merge_keys: dict[str, str] = {}
        set_props: dict[str, str] = {}

        for prop_name, src_col in rm.node_columns.items():
            val = row.get(src_col)
            formatted = format_cypher_value(val, "string")
            if formatted is None:
                continue
            if prop_name in node_def.merge_keys:
                merge_keys[prop_name] = formatted
            else:
                set_props[prop_name] = formatted

        if not merge_keys:
            continue

        # 동일 노드 중복 방지
        dedup_key = (rm.target_label, tuple(sorted(merge_keys.items())))
        if dedup_key in seen:
            continue
        seen.add(dedup_key)

        cyphers.append(_build_merge_node(rm.target_label, merge_keys, set_props))
    return cyphers


def _process_row_relationships(
    row: dict, mapping: MappingResult, part_pn: str
) -> list[str]:
    """행에서 비-CONSISTS_OF 관계 MERGE Cypher 생성.

    CONSISTS_OF는 part_repo.upsert_bom_link에서 처리하므로 제외합니다.
    from = 주인공 Part, to = 상대방 노드 (Supplier, Drawing 등)
    """
    cyphers: list[str] = []

    escaped_pn = format_cypher_value(part_pn, "string")
    if escaped_pn is None:
        return []

    for rm in mapping.relation_mappings:
        if rm.rel_type == "CONSISTS_OF":
            continue

        target_def = MANUFACTURING_ONTOLOGY.get_node_label(rm.target_label)
        if not target_def:
            continue

        # 상대방 노드의 merge key 추출
        to_keys: dict[str, str] = {}
        for prop_name, src_col in rm.node_columns.items():
            val = row.get(src_col)
            formatted = format_cypher_value(val, "string")
            if formatted is None:
                continue
            if prop_name in target_def.merge_keys:
                to_keys[prop_name] = formatted

        if not to_keys:
            continue

        # 관계 속성 추출
        rel_props: dict[str, str] = {}
        for rel_prop, src_col in rm.rel_columns.items():
            data_type = rm.rel_column_types.get(rel_prop, "string")
            formatted = format_cypher_value(row.get(src_col), data_type)
            if formatted is not None:
                rel_props[rel_prop] = formatted

        from_keys = {"part_number": escaped_pn}
        cyphers.append(
            _build_merge_rel(
                "Part", from_keys, rm.target_label, to_keys,
                rm.rel_type, rel_props,
            )
        )
    return cyphers


# ── 백그라운드 합성 ──


def _run_synthesis(
    job_id: uuid.UUID,
    schema_name: str,
    graph_name: str,
    file_key: str,
    filename: str,
    sheet_name: str | None,
    mapping_json: dict,
    root_part_number: str | None = None,
) -> None:
    db = create_tenant_session(schema_name)
    try:
        job = repo.get_synthesis_job_required(db, job_id)
        job.status = "PROCESSING"
        job.started_at = datetime.now(timezone.utc)
        db.commit()

        content = _s3.get_object(file_key)

        # sheet_name=None이면 모든 시트 처리, 특정 시트면 해당 시트만
        sheet_names_list = get_sheet_names(content, filename)
        is_excel = len(sheet_names_list) > 0

        if sheet_name is not None:
            target_sheets = [sheet_name]
        elif is_excel:
            target_sheets = sheet_names_list
        else:
            target_sheets = [None]

        # 모든 시트의 DataFrame을 합산
        dfs: list[pd.DataFrame] = []
        for target in target_sheets:
            try:
                sheet_df = read_to_dataframe(content, filename, sheet_name=target)
                if not sheet_df.empty:
                    dfs.append(sheet_df)
            except Exception as e:
                sheet_label = target or filename
                logger.warning("시트 스킵: {sheet} - {err}", sheet=sheet_label, err=e)

        if not dfs:
            job.total_rows = 0
            job.status = "COMPLETED"
            job.completed_at = datetime.now(timezone.utc)
            db.commit()
            return

        df = pd.concat(dfs, ignore_index=True)

        total_rows = len(df)
        job.total_rows = total_rows
        db.commit()

        if total_rows == 0:
            job.status = "COMPLETED"
            job.completed_at = datetime.now(timezone.utc)
            db.commit()
            return

        mapping = MappingResult(**mapping_json)

        # Root-Specified BOM: rootless CONSISTS_OF 탐지
        rootless_consists_of: RelationMapping | None = None
        if root_part_number:
            for rm in mapping.relation_mappings:
                if rm.rel_type == "CONSISTS_OF" and not rm.node_columns:
                    rootless_consists_of = rm
                    break

        processed = 0
        nodes_created = 0
        rels_created = 0
        errors: list[str] = []

        for chunk_start in range(0, total_rows, CHUNK_SIZE):
            chunk_end = min(chunk_start + CHUNK_SIZE, total_rows)
            chunk = df.iloc[chunk_start:chunk_end]
            t_chunk = time.perf_counter()

            # === Phase 1: 데이터 수집 및 Part별 집계 (first-non-null) ===
            part_data: dict[str, dict] = {}
            bom_entries: list[dict] = []
            all_node_cyphers: list[str] = []
            all_rel_cyphers: list[str] = []

            for idx, (_, row_series) in enumerate(chunk.iterrows()):
                row_num = chunk_start + idx + 1
                row = row_series.to_dict()
                try:
                    # 주인공 Part 속성 추출 및 집계
                    pn, props = _extract_row_part(row, mapping)
                    if pn:
                        _merge_part_props(part_data, pn, props)

                    # CONSISTS_OF 상대방(상위) Part 속성 추출 및 집계
                    related = _extract_related_parts(row, mapping)
                    for related_pn, related_props in related.items():
                        _merge_part_props(part_data, related_pn, related_props)

                    # BOM 데이터 수집
                    bom_entries.extend(_extract_bom_data(row, mapping, pn))

                    # Root-Specified BOM: root_part_number를 상위 Part로 고정
                    if rootless_consists_of and pn and pn != root_part_number:
                        entry: dict = {"parent_pn": root_part_number, "child_pn": pn}
                        for rel_prop, src_col in rootless_consists_of.rel_columns.items():
                            data_type = rootless_consists_of.rel_column_types.get(rel_prop, "string")
                            val = _cast_python_value(row.get(src_col), data_type)
                            if val is not None:
                                entry[rel_prop] = val
                        bom_entries.append(entry)

                    # 비-Part 노드 Cypher 수집
                    all_node_cyphers.extend(_process_row_nodes(row, mapping))

                    # 비-CONSISTS_OF 관계 Cypher 수집
                    if pn:
                        all_rel_cyphers.extend(
                            _process_row_relationships(row, mapping, pn)
                        )
                except Exception as error:
                    err_msg = f"행 {row_num}: {error}"
                    errors.append(err_msg)
                    logger.warning("합성 행 처리 오류: {err}", err=err_msg)

                processed += 1

            # Root-Specified BOM: root part를 part_data에 등록
            if rootless_consists_of and root_part_number:
                _merge_part_props(part_data, root_part_number, {"part_number": root_part_number})

            # === Phase 2: Part upsert (RDS + Graph dual-write) ===
            for pn, props in part_data.items():
                try:
                    part_repo.upsert_part(db, pn, props, job_id, graph_name)
                    nodes_created += 1
                except Exception as error:
                    errors.append(f"Part upsert 실패 ({pn}): {error}")
                    logger.warning(
                        "Part upsert 오류: pn={pn} error={err}", pn=pn, err=error
                    )

            # === Phase 3: 비-Part 노드 (Graph only) ===
            if all_node_cyphers:
                repo.execute_graph_cyphers(db, graph_name, all_node_cyphers)
                nodes_created += len(all_node_cyphers)

            # === Phase 4: BOM 링크 (RDS + Graph dual-write) ===
            for entry in bom_entries:
                quantity = entry.get("quantity", 1)
                ext_props = {
                    k: v for k, v in entry.items()
                    if k not in {"parent_pn", "child_pn", "quantity"}
                }
                try:
                    part_repo.upsert_bom_link(
                        db, graph_name,
                        entry["parent_pn"], entry["child_pn"],
                        quantity,
                        extended_properties=ext_props if ext_props else None,
                    )
                    rels_created += 1
                except part_repo.MissingPartForBomError as error:
                    logger.warning(
                        "합성 BOM 링크 스킵: parent={parent} child={child}",
                        parent=error.parent_pn,
                        child=error.child_pn,
                    )

            # === Phase 5: 비-CONSISTS_OF 관계 (Graph only) ===
            if all_rel_cyphers:
                repo.execute_graph_cyphers(db, graph_name, all_rel_cyphers)
                rels_created += len(all_rel_cyphers)

            db.commit()
            chunk_elapsed = time.perf_counter() - t_chunk
            job.processed_rows = processed
            job.nodes_created = nodes_created
            job.relationships_created = rels_created
            job.errors = errors[:100]
            db.commit()

            logger.info(
                "합성 진행: job_id={job_id} {processed}/{total}행 청크 {elapsed:.1f}s ({rate:.0f}행/s)",
                job_id=job_id,
                processed=processed,
                total=total_rows,
                elapsed=chunk_elapsed,
                rate=len(chunk) / chunk_elapsed if chunk_elapsed > 0 else 0,
            )

        job.status = "COMPLETED"
        job.completed_at = datetime.now(timezone.utc)
        db.commit()
        logger.info(
            "합성 완료: job_id={job_id} 노드={nodes} 관계={rels} 에러={errs}",
            job_id=job_id,
            nodes=nodes_created,
            rels=rels_created,
            errs=len(errors),
        )

    except Exception as error:
        logger.error("합성 실패: job_id={job_id} error={err}", job_id=job_id, err=error)
        try:
            db.rollback()
            job = repo.get_synthesis_job_required(db, job_id)
            job.status = "FAILED"
            job.errors = [str(error)]
            job.completed_at = datetime.now(timezone.utc)
            db.commit()
        except Exception:
            logger.error("합성 실패 상태 저장 오류: job_id={job_id}", job_id=job_id)
    finally:
        db.close()


# ── 헬퍼 ──


def _to_job_response(job: SynthesisJob) -> SynthesisJobResponse:
    return SynthesisJobResponse(
        id=job.id,
        mapping_id=job.mapping_id,
        upload_id=job.upload_id,
        status=job.status,
        total_rows=job.total_rows,
        processed_rows=job.processed_rows,
        nodes_created=job.nodes_created,
        relationships_created=job.relationships_created,
        errors=job.errors,
        started_at=job.started_at,
        completed_at=job.completed_at,
        created_at=job.created_at,
    )


def _cast_python_value(value, data_type: str):
    """Cypher가 아닌 Python 원시값으로 변환"""
    if pd.isna(value) or value is None or str(value).strip() == "":
        return None

    if data_type == "integer":
        try:
            return int(float(value))
        except (ValueError, TypeError):
            return str(value).strip()

    if data_type == "float":
        try:
            return float(value)
        except (ValueError, TypeError):
            return str(value).strip()

    if data_type == "boolean":
        s = str(value).strip().lower()
        if s in ("true", "1", "yes", "y"):
            return True
        if s in ("false", "0", "no", "n"):
            return False
        return str(value).strip()

    return str(value).strip()
