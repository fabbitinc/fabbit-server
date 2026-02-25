"""합성 도메인 서비스 레이어."""

import time
import uuid
import pandas as pd
from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.background_worker import guarded
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.core.uow import UnitOfWork
from app.infrastructure.excel_parser import get_sheet_names, read_to_dataframe
from app.infrastructure.s3_client import s3_client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.cypher_utils import format_cypher_value
from app.modules.ontology.schemas import MappingResult, RelationMapping
from app.modules.drawing import repository as drawing_repo
from app.modules.part import repository as part_repo
from app.modules.supplier import repository as supplier_repo
from app.modules.synthesis import repository as repo
from app.modules.synthesis.models import SynthesisJob
from app.modules.mapping.constants import MappingScope
from app.modules.synthesis.schemas import (
    SynthesisBatchFailure,
    SynthesisBatchItemStatus,
    SynthesisBatchStartResponse,
    SynthesisBatchStatusResponse,
    SynthesisJobResponse,
    SynthesisListResponse,
    SynthesisStartRequest,
    SynthesisUploadItem,
)

_s3 = s3_client

CHUNK_SIZE = 500


def _get_rootless_labels(mapping: MappingResult) -> set[str]:
    """매핑에서 rootless relation의 target_label 집합 추출."""
    return {
        rm.target_label
        for rm in mapping.relation_mappings
        if not rm.node_columns and rm.rel_columns
    }


def _validate_root_context(
    scope: str,
    root_context: dict[str, str] | None,
    required_labels: set[str],
) -> None:
    """scope 기반 root_context 검증."""
    if scope in (MappingScope.PART_LIST, MappingScope.FULL_BOM):
        if root_context:
            raise AppError(
                message="이 매핑은 root_context가 필요하지 않습니다.",
                code="UNEXPECTED_ROOT_CONTEXT",
            )
    elif scope == MappingScope.ROOT_BOM:
        if not root_context:
            raise AppError(
                message="이 매핑은 ROOT_BOM입니다. root_context를 지정해주세요.",
                code="MISSING_ROOT_CONTEXT",
            )
        missing = required_labels - root_context.keys()
        if missing:
            raise AppError(
                message=f"root_context에 필요한 키가 누락되었습니다: {', '.join(sorted(missing))}",
                code="MISSING_ROOT_CONTEXT",
            )


@transactional
def start_synthesis(
    db: Session,
    auth: AuthContext,
    req: SynthesisStartRequest,
    add_background_task,
) -> SynthesisBatchStartResponse:
    # 1. 매핑 조회
    record = repo.get_mapping_by_id(db, req.mapping_id)
    if record is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")

    # 2. 최신 리비전 조회
    revision = repo.get_latest_revision(db, record.id)
    if revision is None:
        raise AppError(message="매핑 리비전을 찾을 수 없습니다", code="NOT_FOUND")

    # 3. 프로젝트 존재 검증
    if req.project_id is not None:
        project = repo.get_project_by_id(db, req.project_id)
        if project is None:
            raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    # 4. 모든 upload에 대해 root_context 검증 (scope 기반)
    mapping_for_scope = MappingResult(**revision.mapping)
    required_labels = _get_rootless_labels(mapping_for_scope)
    for item in req.uploads:
        _validate_root_context(record.scope, item.root_context, required_labels)

    # 5. upload 루프
    schema_name = org_id_to_schema(auth.org_id)
    accepted_jobs: list[tuple[SynthesisJob, object, SynthesisUploadItem]] = []
    failed: list[SynthesisBatchFailure] = []

    for item in req.uploads:
        file = repo.get_file_by_id(db, item.file_id)
        if file is None:
            failed.append(
                SynthesisBatchFailure(
                    file_id=item.file_id,
                    reason="파일을 찾을 수 없습니다",
                )
            )
            continue

        if file.status != "UPLOADED":
            failed.append(
                SynthesisBatchFailure(
                    file_id=item.file_id,
                    reason="업로드가 완료되지 않은 파일입니다",
                )
            )
            continue

        if req.project_id is not None and (
            file.owner_type != "project" or file.owner_id != req.project_id
        ):
            failed.append(
                SynthesisBatchFailure(
                    file_id=item.file_id,
                    reason="해당 프로젝트에 속하지 않은 파일입니다",
                )
            )
            continue

        job = repo.create_synthesis_job(
            db=db,
            mapping_id=record.id,
            file_id=file.id,
        )
        accepted_jobs.append((job, file, item))

    # 6. SynthesisBatch 생성
    batch = repo.create_synthesis_batch(
        db=db,
        batch_id=generate_uuid7(),
        project_id=req.project_id,
        mapping_id=record.id,
        requested_count=len(req.uploads),
        accepted_count=len(accepted_jobs),
        failed_uploads=[f.model_dump(mode="json") for f in failed],
    )

    # 7. 성공 job들에 batch_id 할당
    for job, _file, _item in accepted_jobs:
        job.assign_batch(batch.id)

    # 8. usage_count 일괄 증가
    if accepted_jobs:
        repo.increment_mapping_usage(db, record, revision, len(accepted_jobs))

    db.flush()
    db.refresh(batch)
    for job, _file, _item in accepted_jobs:
        db.refresh(job)

    # 9. 각 job에 대해 background task 등록
    for job, file, item in accepted_jobs:
        add_background_task(
            guarded(_run_synthesis),
            job_id=job.id,
            schema_name=schema_name,
            graph_name=schema_name,
            file_key=file.file_key,
            filename=file.original_name,
            sheet_name=revision.sheet_name,
            mapping_json=revision.mapping,
            root_context=item.root_context,
            overwrite=req.overwrite,
        )

    logger.info(
        "합성 시작: batch_id={batch_id} mapping_id={mapping_id} accepted={accepted} failed={failed}",
        batch_id=batch.id,
        mapping_id=record.id,
        accepted=len(accepted_jobs),
        failed=len(failed),
    )
    # 10. 항상 배치 응답 반환
    return SynthesisBatchStartResponse(
        batch_id=batch.id,
        requested_count=batch.requested_count,
        accepted_count=batch.accepted_count,
        items=[_to_job_response(job) for job, _file, _item in accepted_jobs],
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
                file_id=job.file_id,
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


def _extract_drawing_supplier_nodes(
    row: dict, mapping: MappingResult
) -> tuple[dict[str, dict], dict[str, dict]]:
    """행에서 Drawing/Supplier 노드 속성 추출.

    Returns:
        (drawing_props, supplier_props) — {merge_key_value: {속성dict}}
    """
    drawing_props: dict[str, dict] = {}
    supplier_props: dict[str, dict] = {}

    for rm in mapping.relation_mappings:
        if rm.target_label not in ("Drawing", "Supplier"):
            continue

        props: dict = {}
        for prop_name, src_col in rm.node_columns.items():
            val = _cast_python_value(row.get(src_col), "string")
            if val is not None:
                props[prop_name] = val

        if rm.target_label == "Drawing":
            dn = props.get("drawing_number")
            if dn:
                drawing_props[str(dn)] = props
        elif rm.target_label == "Supplier":
            cn = props.get("company_name")
            if cn:
                supplier_props[str(cn)] = props

    return drawing_props, supplier_props


def _process_row_nodes(row: dict, mapping: MappingResult) -> list[str]:
    """행에서 비-Part/Drawing/Supplier 노드 MERGE Cypher 생성.

    Part, Drawing, Supplier는 RDS dual-write 경로에서 처리합니다.
    """
    cyphers: list[str] = []
    seen: set[tuple] = set()

    for rm in mapping.relation_mappings:
        if rm.target_label in ("Part", "Drawing", "Supplier"):
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
    """행에서 비-CONSISTS_OF/DEFINED_BY/SUPPLIED_BY 관계 MERGE Cypher 생성.

    CONSISTS_OF는 part_repo.upsert_bom_link에서 처리합니다.
    DEFINED_BY/SUPPLIED_BY는 dual-write 경로에서 처리합니다.
    """
    cyphers: list[str] = []

    escaped_pn = format_cypher_value(part_pn, "string")
    if escaped_pn is None:
        return []

    for rm in mapping.relation_mappings:
        if rm.rel_type in ("CONSISTS_OF", "DEFINED_BY", "SUPPLIED_BY"):
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


def _extract_defined_by(
    row: dict, mapping: MappingResult
) -> dict[str, str]:
    """행에서 DEFINED_BY 관계 데이터 추출.

    Returns:
        {part_number: drawing_number} — 관계가 없으면 빈 dict
    """
    result: dict[str, str] = {}
    for rm in mapping.relation_mappings:
        if rm.rel_type != "DEFINED_BY" or rm.target_label != "Drawing":
            continue
        dn_col = rm.node_columns.get("drawing_number")
        if not dn_col:
            continue
        dn = _cast_python_value(row.get(dn_col), "string")
        if dn:
            result["drawing_number"] = str(dn)
    return result


def _extract_supplied_by(
    row: dict, mapping: MappingResult
) -> list[dict]:
    """행에서 SUPPLIED_BY 관계 데이터 추출.

    Returns:
        [{"company_name": "...", "unit_cost": ..., ...}]
    """
    entries: list[dict] = []
    for rm in mapping.relation_mappings:
        if rm.rel_type != "SUPPLIED_BY" or rm.target_label != "Supplier":
            continue
        cn_col = rm.node_columns.get("company_name")
        if not cn_col:
            continue
        cn = _cast_python_value(row.get(cn_col), "string")
        if not cn:
            continue
        entry: dict = {"company_name": str(cn)}
        for rel_prop, src_col in rm.rel_columns.items():
            data_type = rm.rel_column_types.get(rel_prop, "string")
            val = _cast_python_value(row.get(src_col), data_type)
            if val is not None:
                entry[rel_prop] = val
        entries.append(entry)
    return entries


# ── 백그라운드 합성 ──


def _run_synthesis(
    job_id: uuid.UUID,
    schema_name: str,
    graph_name: str,
    file_key: str,
    filename: str,
    sheet_name: str | None,
    mapping_json: dict,
    root_context: dict[str, str] | None = None,
    overwrite: bool = False,
) -> None:
    db = create_tenant_session(schema_name)
    try:
        job = repo.get_synthesis_job_required(db, job_id)
        job.start_processing()
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
            job.complete_empty()
            db.commit()
            return

        df = pd.concat(dfs, ignore_index=True)

        total_rows = len(df)
        job.set_total_rows(total_rows)
        db.commit()

        if total_rows == 0:
            job.complete_empty()
            db.commit()
            return

        mapping = MappingResult(**mapping_json)

        # Rootless relation 탐지: node_columns 빈 모든 relation 수집
        rootless_rels: list[RelationMapping] = []
        if root_context:
            for rm in mapping.relation_mappings:
                if not rm.node_columns and rm.rel_columns:
                    rootless_rels.append(rm)

        processed = 0
        nodes_created = 0
        rels_created = 0
        errors: list[str] = []

        for chunk_start in range(0, total_rows, CHUNK_SIZE):
            chunk_end = min(chunk_start + CHUNK_SIZE, total_rows)
            chunk = df.iloc[chunk_start:chunk_end]
            t_chunk = time.perf_counter()

            # === Phase 1: 데이터 수집 및 노드별 집계 (first-non-null) ===
            part_data: dict[str, dict] = {}
            drawing_data: dict[str, dict] = {}
            supplier_data: dict[str, dict] = {}
            bom_entries: list[dict] = []
            part_drawing_map: dict[str, str] = {}
            part_supplier_entries: list[dict] = []
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

                    # Rootless relation 처리
                    for rm in rootless_rels:
                        if not pn:
                            break
                        root_value = root_context.get(rm.target_label)
                        if not root_value:
                            continue

                        if rm.rel_type == "CONSISTS_OF":
                            # BOM dual-write 경로: root Part를 상위로 고정
                            if pn != root_value:
                                entry: dict = {"parent_pn": root_value, "child_pn": pn}
                                for rel_prop, src_col in rm.rel_columns.items():
                                    data_type = rm.rel_column_types.get(rel_prop, "string")
                                    val = _cast_python_value(row.get(src_col), data_type)
                                    if val is not None:
                                        entry[rel_prop] = val
                                bom_entries.append(entry)
                        elif rm.rel_type == "DEFINED_BY":
                            # DEFINED_BY dual-write 경로
                            part_drawing_map[pn] = root_value
                        elif rm.rel_type == "SUPPLIED_BY":
                            # SUPPLIED_BY dual-write 경로
                            sup_entry: dict = {"part_pn": pn, "company_name": root_value}
                            for rel_prop, src_col in rm.rel_columns.items():
                                data_type = rm.rel_column_types.get(rel_prop, "string")
                                val = _cast_python_value(row.get(src_col), data_type)
                                if val is not None:
                                    sup_entry[rel_prop] = val
                            part_supplier_entries.append(sup_entry)
                        else:
                            # 기타 관계: Graph Cypher 폴백
                            target_def = MANUFACTURING_ONTOLOGY.get_node_label(rm.target_label)
                            if not target_def:
                                continue
                            merge_key = target_def.merge_keys[0]
                            rel_props: dict[str, str] = {}
                            for rel_prop, src_col in rm.rel_columns.items():
                                data_type = rm.rel_column_types.get(rel_prop, "string")
                                formatted = format_cypher_value(row.get(src_col), data_type)
                                if formatted is not None:
                                    rel_props[rel_prop] = formatted
                            from_keys = {"part_number": format_cypher_value(pn, "string")}
                            to_keys = {merge_key: format_cypher_value(root_value, "string")}
                            all_rel_cyphers.append(
                                _build_merge_rel("Part", from_keys, rm.target_label, to_keys, rm.rel_type, rel_props)
                            )

                    # Drawing/Supplier 속성 수집 (RDS dual-write 경로)
                    row_drawing, row_supplier = _extract_drawing_supplier_nodes(row, mapping)
                    for dn, d_props in row_drawing.items():
                        _merge_part_props(drawing_data, dn, d_props)
                    for cn, s_props in row_supplier.items():
                        _merge_part_props(supplier_data, cn, s_props)

                    # DEFINED_BY / SUPPLIED_BY 관계 데이터 수집 (dual-write 경로)
                    if pn:
                        defined_by = _extract_defined_by(row, mapping)
                        if defined_by.get("drawing_number"):
                            part_drawing_map[pn] = defined_by["drawing_number"]

                        for se in _extract_supplied_by(row, mapping):
                            part_supplier_entries.append(
                                {"part_pn": pn, **se}
                            )

                    # 미지 노드 Cypher 수집 (Drawing/Supplier 제외)
                    all_node_cyphers.extend(_process_row_nodes(row, mapping))

                    # 비-CONSISTS_OF/DEFINED_BY/SUPPLIED_BY 관계 Cypher 수집
                    if pn:
                        all_rel_cyphers.extend(
                            _process_row_relationships(row, mapping, pn)
                        )
                except Exception as error:
                    err_msg = f"행 {row_num}: {error}"
                    errors.append(err_msg)
                    logger.warning("합성 행 처리 오류: {err}", err=err_msg)

                processed += 1

            # Rootless relation: 외부 노드 등록
            if root_context:
                for rm in rootless_rels:
                    root_value = root_context.get(rm.target_label)
                    if not root_value:
                        continue
                    if rm.rel_type == "CONSISTS_OF":
                        # Part는 RDS dual-write 경로
                        _merge_part_props(part_data, root_value, {"part_number": root_value})
                    elif rm.target_label == "Drawing":
                        _merge_part_props(drawing_data, root_value, {"drawing_number": root_value})
                    elif rm.target_label == "Supplier":
                        _merge_part_props(supplier_data, root_value, {"company_name": root_value})
                    else:
                        # 미지 노드: Graph MERGE 폴백
                        target_def = MANUFACTURING_ONTOLOGY.get_node_label(rm.target_label)
                        if target_def:
                            merge_key = target_def.merge_keys[0]
                            all_node_cyphers.append(
                                _build_merge_node(rm.target_label, {merge_key: format_cypher_value(root_value, "string")}, {})
                            )

            # === Phase 2: Part upsert (RDS + Graph dual-write) ===
            for pn, props in part_data.items():
                try:
                    part_repo.upsert_part(
                        db, pn, props, job_id, graph_name, overwrite=overwrite
                    )
                    nodes_created += 1
                except Exception as error:
                    errors.append(f"Part upsert 실패 ({pn}): {error}")
                    logger.warning(
                        "Part upsert 오류: pn={pn} error={err}", pn=pn, err=error
                    )

            # === Phase 3a: Drawing upsert (RDS + Graph dual-write) ===
            for dn, d_props in drawing_data.items():
                try:
                    drawing_repo.upsert_drawing(
                        db, dn, d_props, graph_name, overwrite=overwrite
                    )
                    nodes_created += 1
                except Exception as error:
                    errors.append(f"Drawing upsert 실패 ({dn}): {error}")
                    logger.warning(
                        "Drawing upsert 오류: dn={dn} error={err}", dn=dn, err=error
                    )

            # === Phase 3b: Supplier upsert (RDS + Graph dual-write) ===
            for cn, s_props in supplier_data.items():
                try:
                    supplier_repo.upsert_supplier(
                        db, cn, s_props, graph_name, overwrite=overwrite
                    )
                    nodes_created += 1
                except Exception as error:
                    errors.append(f"Supplier upsert 실패 ({cn}): {error}")
                    logger.warning(
                        "Supplier upsert 오류: cn={cn} error={err}", cn=cn, err=error
                    )

            # === Phase 3c: 미지 노드 (Graph only 폴백) ===
            if all_node_cyphers:
                unique_node_cyphers = list(dict.fromkeys(all_node_cyphers))
                repo.execute_graph_cyphers(db, graph_name, unique_node_cyphers)
                nodes_created += len(unique_node_cyphers)

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
                        overwrite=overwrite,
                    )
                    rels_created += 1
                except part_repo.MissingPartForBomError as error:
                    logger.warning(
                        "합성 BOM 링크 스킵: parent={parent} child={child}",
                        parent=error.parent_pn,
                        child=error.child_pn,
                    )

            # === Phase 5a: DEFINED_BY (RDS + Graph dual-write) ===
            for pn, dn in part_drawing_map.items():
                try:
                    part_repo.link_part_to_drawing(db, graph_name, pn, dn)
                    rels_created += 1
                except Exception as error:
                    errors.append(f"DEFINED_BY 실패 ({pn}→{dn}): {error}")

            # === Phase 5b: SUPPLIED_BY (RDS + Graph dual-write) ===
            for s_entry in part_supplier_entries:
                try:
                    part_repo.link_part_to_supplier(
                        db,
                        graph_name,
                        s_entry["part_pn"],
                        s_entry["company_name"],
                        unit_cost=s_entry.get("unit_cost"),
                        extended_properties={
                            k: v
                            for k, v in s_entry.items()
                            if k not in {"part_pn", "company_name", "unit_cost"}
                        } or None,
                        overwrite=overwrite,
                    )
                    rels_created += 1
                except Exception as error:
                    errors.append(
                        f"SUPPLIED_BY 실패 ({s_entry['part_pn']}→{s_entry['company_name']}): {error}"
                    )

            # === Phase 5c: 기타 관계 (Graph only 폴백) ===
            if all_rel_cyphers:
                unique_rel_cyphers = list(dict.fromkeys(all_rel_cyphers))
                repo.execute_graph_cyphers(db, graph_name, unique_rel_cyphers)
                rels_created += len(unique_rel_cyphers)

            db.commit()
            chunk_elapsed = time.perf_counter() - t_chunk
            job.update_progress(
                processed_rows=processed,
                nodes_created=nodes_created,
                relationships_created=rels_created,
                errors=errors,
            )
            db.commit()

            logger.info(
                "합성 진행: job_id={job_id} {processed}/{total}행 청크 {elapsed:.1f}s ({rate:.0f}행/s)",
                job_id=job_id,
                processed=processed,
                total=total_rows,
                elapsed=chunk_elapsed,
                rate=len(chunk) / chunk_elapsed if chunk_elapsed > 0 else 0,
            )

        job.complete()
        UnitOfWork(db).commit()
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
            job.fail(errors=[str(error)])
            UnitOfWork(db).commit()
        except Exception:
            logger.error("합성 실패 상태 저장 오류: job_id={job_id}", job_id=job_id)
    finally:
        db.close()


# ── 헬퍼 ──


def _to_job_response(job: SynthesisJob) -> SynthesisJobResponse:
    return SynthesisJobResponse(
        id=job.id,
        mapping_id=job.mapping_id,
        file_id=job.file_id,
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
