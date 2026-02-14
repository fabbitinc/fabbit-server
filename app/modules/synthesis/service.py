"""합성 도메인 서비스 레이어."""

import time
import uuid
from datetime import datetime, timezone

import pandas as pd
from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import SessionLocal, generate_uuid7
from app.core.exceptions import AppError
from app.infrastructure.age_client import execute_cypher_raw
from app.infrastructure.excel_parser import get_sheet_names, read_to_dataframe
from app.infrastructure.s3_client import S3Client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.repository import format_cypher_value
from app.modules.ontology.schemas import MappingResult
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
    repo.increment_mapping_usage(record)
    db.commit()
    db.refresh(job)

    schema_name = org_id_to_schema(auth.org_id)
    add_background_task(
        _run_synthesis,
        job_id=job.id,
        schema_name=schema_name,
        graph_name=schema_name,
        file_key=upload.file_key,
        filename=upload.original_name,
        sheet_name=record.sheet_name,
        mapping_json=record.mapping,
    )

    logger.info(
        "합성 작업 시작: job_id={job_id} mapping_id={mapping_id} upload_id={upload_id}",
        job_id=job.id,
        mapping_id=record.id,
        upload_id=upload.id,
    )
    return _to_job_response(job)


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

        if upload.project_id != project_id:
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
        repo.increment_mapping_usage(record, len(accepted_jobs))

    db.commit()
    db.refresh(batch)
    for job, _upload in accepted_jobs:
        db.refresh(job)

    for job, upload in accepted_jobs:
        add_background_task(
            _run_synthesis,
            job_id=job.id,
            schema_name=schema_name,
            graph_name=schema_name,
            file_key=upload.file_key,
            filename=upload.original_name,
            sheet_name=record.sheet_name,
            mapping_json=record.mapping,
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


def get_synthesis_job(db: Session, job_id: uuid.UUID) -> SynthesisJobResponse:
    job = repo.get_synthesis_job_by_id(db, job_id)
    if job is None:
        raise AppError(message="합성 작업을 찾을 수 없습니다", code="NOT_FOUND")
    return _to_job_response(job)


def list_synthesis_jobs(db: Session) -> SynthesisListResponse:
    jobs = repo.list_synthesis_jobs(db)
    return SynthesisListResponse(items=[_to_job_response(j) for j in jobs])


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


def _process_row_nodes(
    row: dict, mapping: MappingResult, skip_labels: set[str] | None = None
) -> list[str]:
    cyphers: list[str] = []

    # 분리 판별: column_mappings에서 같은 (label, property)에 복수 source_column이 있으면 분리 필요
    prop_sources: dict[tuple[str, str], list[str]] = {}
    for cm in mapping.column_mappings:
        key = (cm.target_label, cm.target_property)
        prop_sources.setdefault(key, []).append(cm.source_column)

    label_needs_split: set[str] = set()
    for (label, _prop), sources in prop_sources.items():
        if len(sources) >= 2:
            label_needs_split.add(label)

    # self-join 관계(from_label == to_label)에서만 from/to role 수집
    # 다른 라벨 간 관계(SUPPLIED_BY 등)는 분리 판별에 영향 주지 않음
    endpoint_roles: dict[tuple[str, str], str] = {}
    for rm in mapping.relation_mappings:
        if rm.from_label != rm.to_label:
            continue
        for _mk, src_col in rm.from_columns.items():
            endpoint_roles[(rm.from_label, src_col)] = "from"
        for _mk, src_col in rm.to_columns.items():
            endpoint_roles[(rm.to_label, src_col)] = "to"

    # 분리 대상 라벨에서 동일 target_property에 중복 매핑된 source_column을 role로 분류
    # 예: (Part, name) → [상위품명, 하위품명] → 상위품명은 from, 하위품명은 to
    _assign_duplicate_column_roles(mapping, endpoint_roles, label_needs_split)

    # 라벨별 속성 수집 (분리가 필요한 라벨은 from/to 별도 dict)
    # 키: (label, role) — role은 "from", "to", 또는 "default"
    grouped_props: dict[tuple[str, str], dict[str, str]] = {}

    for cm in mapping.column_mappings:
        val = row.get(cm.source_column)
        formatted = format_cypher_value(val, cm.data_type)
        if formatted is None:
            continue

        label = cm.target_label
        if label in label_needs_split:
            role = endpoint_roles.get((label, cm.source_column))
            if role:
                grouped_props.setdefault((label, role), {})[cm.target_property] = (
                    formatted
                )
            else:
                # 엔드포인트에 속하지 않는 고유 속성 → from/to 양쪽에 추가
                for r in ("from", "to"):
                    grouped_props.setdefault((label, r), {})[cm.target_property] = (
                        formatted
                    )
        else:
            grouped_props.setdefault((label, "default"), {})[cm.target_property] = (
                formatted
            )

    for ep in mapping.extended_properties:
        val = row.get(ep.source_column)
        formatted = format_cypher_value(val, ep.data_type)
        if formatted is None:
            continue

        label = ep.target_label
        if label in label_needs_split:
            role = endpoint_roles.get((label, ep.source_column))
            if role:
                grouped_props.setdefault((label, role), {})[ep.property_name] = (
                    formatted
                )
            else:
                for r in ("from", "to"):
                    grouped_props.setdefault((label, r), {})[ep.property_name] = (
                        formatted
                    )
        else:
            grouped_props.setdefault((label, "default"), {})[ep.property_name] = (
                formatted
            )

    for (label, _role), props in grouped_props.items():
        node_def = MANUFACTURING_ONTOLOGY.get_node_label(label)
        if node_def is None:
            continue
        if skip_labels and label in skip_labels:
            continue

        merge_keys: dict[str, str] = {}
        set_props: dict[str, str] = {}
        for mk in node_def.merge_keys:
            if mk in props:
                merge_keys[mk] = props[mk]

        if not merge_keys:
            continue

        for key, value in props.items():
            if key not in merge_keys:
                set_props[key] = value
        cyphers.append(_build_merge_node(label, merge_keys, set_props))

    return cyphers


def _assign_duplicate_column_roles(
    mapping: MappingResult,
    endpoint_roles: dict[tuple[str, str], str],
    label_needs_split: set[str],
) -> None:
    """동일 target_property에 복수 source_column이 매핑된 경우 from/to role을 추론.

    예: Part.name에 "상위품명"과 "하위품명"이 모두 매핑된 경우,
    from_columns에 있는 "상위품번"과 같은 관계의 from 쪽 컬럼 그룹에서
    "상위품명"을 찾아 from role을 부여.
    """
    # 분리 대상 라벨에서 중복 매핑 탐색
    # (label, target_property) → [source_column, ...]
    prop_sources: dict[tuple[str, str], list[str]] = {}
    for cm in mapping.column_mappings:
        if cm.target_label in label_needs_split:
            key = (cm.target_label, cm.target_property)
            prop_sources.setdefault(key, []).append(cm.source_column)

    # 중복 매핑이 없으면 추론 불필요
    duplicates = {k: v for k, v in prop_sources.items() if len(v) >= 2}
    if not duplicates:
        return

    # 관계별로 from/to 소스 컬럼 집합 수집 (role 추론 기준)
    for rm in mapping.relation_mappings:
        from_src_cols = set(rm.from_columns.values())
        to_src_cols = set(rm.to_columns.values())

        for (label, _prop), src_cols in duplicates.items():
            if label != rm.from_label or label != rm.to_label:
                continue
            for src_col in src_cols:
                if (label, src_col) in endpoint_roles:
                    continue  # 이미 할당됨
                # 같은 관계의 from/to 소스 컬럼과 이름 유사도로 그룹 추론
                # 휴리스틱: from_columns의 소스 컬럼과 공통 접두/접미사 공유
                from_score = _column_group_affinity(src_col, from_src_cols)
                to_score = _column_group_affinity(src_col, to_src_cols)
                if from_score > to_score:
                    endpoint_roles[(label, src_col)] = "from"
                elif to_score > from_score:
                    endpoint_roles[(label, src_col)] = "to"


def _column_group_affinity(candidate: str, group_cols: set[str]) -> int:
    """후보 컬럼과 그룹 컬럼들 간의 이름 유사도 점수 계산.

    공통 접두사/접미사 길이 기반 휴리스틱.
    예: "상위품명"과 {"상위품번"} → "상위" 접두사 2자 공유 → 점수 2
    """
    if not group_cols:
        return 0
    best = 0
    for gc in group_cols:
        # 공통 접두사 길이
        prefix_len = 0
        for a, b in zip(candidate, gc):
            if a == b:
                prefix_len += 1
            else:
                break
        # 공통 접미사 길이
        suffix_len = 0
        for a, b in zip(reversed(candidate), reversed(gc)):
            if a == b:
                suffix_len += 1
            else:
                break
        best = max(best, prefix_len + suffix_len)
    return best


def _process_row_relationships(
    row: dict, mapping: MappingResult, skip_rel_types: set[str] | None = None
) -> list[str]:
    cyphers: list[str] = []

    # column_mappings에서 라벨별 merge key 값을 수집 (폴백용)
    label_merge_vals: dict[str, dict[str, str]] = {}
    # source_column → data_type 매핑 (from_columns/to_columns에서 타입 조회용)
    col_data_types: dict[str, str] = {}

    for cm in mapping.column_mappings:
        node_def = MANUFACTURING_ONTOLOGY.get_node_label(cm.target_label)
        if node_def is None:
            continue
        col_data_types[cm.source_column] = cm.data_type
        if cm.target_property in node_def.merge_keys:
            val = row.get(cm.source_column)
            formatted = format_cypher_value(val, cm.data_type)
            if formatted is not None:
                label_merge_vals.setdefault(cm.target_label, {})[cm.target_property] = (
                    formatted
                )

    for rm in mapping.relation_mappings:
        if skip_rel_types and rm.rel_type in skip_rel_types:
            continue
        # from_columns/to_columns가 있으면 독립적으로 merge key 조회
        if rm.from_columns:
            from_keys = _resolve_endpoint_keys(row, rm.from_columns, col_data_types)
        else:
            from_keys = label_merge_vals.get(rm.from_label, {})

        if rm.to_columns:
            to_keys = _resolve_endpoint_keys(row, rm.to_columns, col_data_types)
        else:
            to_keys = label_merge_vals.get(rm.to_label, {})

        if not from_keys or not to_keys:
            continue

        rel_props: dict[str, str] = {}
        for src_col, rel_prop in rm.properties.items():
            val = row.get(src_col)
            data_type = rm.property_types.get(rel_prop, "string")
            formatted = format_cypher_value(val, data_type)
            if formatted is not None:
                rel_props[rel_prop] = formatted

        cyphers.append(
            _build_merge_rel(
                rm.from_label,
                from_keys,
                rm.to_label,
                to_keys,
                rm.rel_type,
                rel_props,
            )
        )
    return cyphers


def _resolve_endpoint_keys(
    row: dict,
    endpoint_columns: dict[str, str],
    col_data_types: dict[str, str],
) -> dict[str, str]:
    """from_columns/to_columns 기반으로 행에서 merge key 값 추출"""
    keys: dict[str, str] = {}
    for merge_key, source_column in endpoint_columns.items():
        val = row.get(source_column)
        data_type = col_data_types.get(source_column, "string")
        formatted = format_cypher_value(val, data_type)
        if formatted is not None:
            keys[merge_key] = formatted
    return keys


def _run_synthesis(
    job_id: uuid.UUID,
    schema_name: str,
    graph_name: str,
    file_key: str,
    filename: str,
    sheet_name: str | None,
    mapping_json: dict,
) -> None:
    db = SessionLocal()
    try:
        repo.set_search_path(db, schema_name)

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
        processed = 0
        nodes_created = 0
        rels_created = 0
        errors: list[str] = []

        for chunk_start in range(0, total_rows, CHUNK_SIZE):
            chunk_end = min(chunk_start + CHUNK_SIZE, total_rows)
            chunk = df.iloc[chunk_start:chunk_end]
            t_chunk = time.perf_counter()

            for idx, (_, row_series) in enumerate(chunk.iterrows()):
                row_num = chunk_start + idx + 1
                row = row_series.to_dict()
                try:
                    # Part → part_repo (RDS + Graph 통합)
                    part_entries = _extract_part_data(row, mapping)
                    for pn, props in part_entries.items():
                        part_repo.upsert_part(db, pn, props, job_id, graph_name)

                    # BOM 관계 → part_repo (RDS + Graph dual-write)
                    bom_entries = _extract_bom_data(row, mapping)
                    for entry in bom_entries:
                        part_repo.upsert_bom_link(
                            db,
                            graph_name,
                            entry["parent_pn"],
                            entry["child_pn"],
                            entry.get("quantity", 1),
                            sequence=entry.get("sequence"),
                            reference_designator=entry.get("reference_designator"),
                            find_number=entry.get("find_number"),
                        )

                    # 비-Part 노드 → Graph only (기존)
                    node_cyphers = _process_row_nodes(
                        row, mapping, skip_labels={"Part"}
                    )
                    for cypher in node_cyphers:
                        execute_cypher_raw(db, cypher, graph_name)
                    nodes_created += len(part_entries) + len(node_cyphers)

                    # 비-CONSISTS_OF 관계 → Graph only (기존)
                    rel_cyphers = _process_row_relationships(
                        row, mapping, skip_rel_types={"CONSISTS_OF"}
                    )
                    for cypher in rel_cyphers:
                        execute_cypher_raw(db, cypher, graph_name)
                    rels_created += len(bom_entries) + len(rel_cyphers)
                except Exception as error:
                    err_msg = f"행 {row_num}: {error}"
                    errors.append(err_msg)
                    logger.warning("합성 행 처리 오류: {err}", err=err_msg)

                processed += 1

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


def _extract_part_data(row: dict, mapping: MappingResult) -> dict[str, dict]:
    """행에서 Part 라벨 속성을 Python 원시값으로 추출.

    반환: {part_number: {속성dict}} — 분리(split) 시 여러 Part가 포함될 수 있음
    """
    # _process_row_nodes와 동일한 분리 판별 로직 재활용
    prop_sources: dict[tuple[str, str], list[str]] = {}
    for cm in mapping.column_mappings:
        key = (cm.target_label, cm.target_property)
        prop_sources.setdefault(key, []).append(cm.source_column)

    label_needs_split: set[str] = set()
    for (label, _prop), sources in prop_sources.items():
        if len(sources) >= 2:
            label_needs_split.add(label)

    endpoint_roles: dict[tuple[str, str], str] = {}
    for rm in mapping.relation_mappings:
        if rm.from_label != rm.to_label:
            continue
        for _mk, src_col in rm.from_columns.items():
            endpoint_roles[(rm.from_label, src_col)] = "from"
        for _mk, src_col in rm.to_columns.items():
            endpoint_roles[(rm.to_label, src_col)] = "to"

    _assign_duplicate_column_roles(mapping, endpoint_roles, label_needs_split)

    # Part 라벨 속성만 수집 (Python 원시값)
    grouped: dict[str, dict[str, object]] = {}  # role → {prop: value}

    for cm in mapping.column_mappings:
        if cm.target_label != "Part":
            continue
        val = _cast_python_value(row.get(cm.source_column), cm.data_type)
        if val is None:
            continue

        if "Part" in label_needs_split:
            role = endpoint_roles.get(("Part", cm.source_column))
            if role:
                grouped.setdefault(role, {})[cm.target_property] = val
            else:
                for r in ("from", "to"):
                    grouped.setdefault(r, {})[cm.target_property] = val
        else:
            grouped.setdefault("default", {})[cm.target_property] = val

    for ep in mapping.extended_properties:
        if ep.target_label != "Part":
            continue
        val = _cast_python_value(row.get(ep.source_column), ep.data_type)
        if val is None:
            continue

        if "Part" in label_needs_split:
            role = endpoint_roles.get(("Part", ep.source_column))
            if role:
                grouped.setdefault(role, {})[ep.property_name] = val
            else:
                for r in ("from", "to"):
                    grouped.setdefault(r, {})[ep.property_name] = val
        else:
            grouped.setdefault("default", {})[ep.property_name] = val

    # part_number 기준으로 결과 구성
    result: dict[str, dict] = {}
    for _role, props in grouped.items():
        pn = props.get("part_number")
        if not pn:
            continue
        result[str(pn)] = props
    return result


def _extract_bom_data(row: dict, mapping: MappingResult) -> list[dict]:
    """행에서 CONSISTS_OF 관계 데이터를 Python 원시값으로 추출.

    반환: [{"parent_pn": "ASM-001", "child_pn": "BRK-001", "quantity": 2, ...}, ...]
    """
    col_data_types: dict[str, str] = {}
    for cm in mapping.column_mappings:
        col_data_types[cm.source_column] = cm.data_type

    entries: list[dict] = []
    for rm in mapping.relation_mappings:
        if rm.rel_type != "CONSISTS_OF":
            continue

        # 엔드포인트 part_number 추출
        parent_pn = _resolve_endpoint_pn(row, rm.from_columns, col_data_types)
        child_pn = _resolve_endpoint_pn(row, rm.to_columns, col_data_types)
        if not parent_pn or not child_pn:
            continue

        entry: dict = {"parent_pn": parent_pn, "child_pn": child_pn}
        for src_col, rel_prop in rm.properties.items():
            data_type = rm.property_types.get(rel_prop, "string")
            parsed = _cast_python_value(row.get(src_col), data_type)
            if parsed is not None:
                entry[rel_prop] = parsed

        entries.append(entry)

    return entries


def _resolve_endpoint_pn(
    row: dict,
    endpoint_columns: dict[str, str],
    col_data_types: dict[str, str],
) -> str | None:
    """엔드포인트 컬럼에서 part_number 값을 Python 문자열로 추출."""
    src_col = endpoint_columns.get("part_number")
    if not src_col:
        return None
    val = row.get(src_col)
    data_type = col_data_types.get(src_col, "string")
    parsed = _cast_python_value(val, data_type)
    return str(parsed) if parsed is not None else None
