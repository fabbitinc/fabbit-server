"""합성(Synthesis) API 라우터.

확정된 매핑을 기반으로 업로드 파일 데이터를 AGE 그래프에 적재합니다.
BackgroundTasks로 비동기 처리하며, SynthesisJob 테이블로 진행 상태를 추적합니다.
"""

import uuid
from datetime import datetime, timezone

import pandas as pd
from fastapi import APIRouter, BackgroundTasks, Depends
from loguru import logger
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.core.database import SessionLocal, generate_uuid7
from app.infrastructure.age_client import execute_cypher_raw
from app.infrastructure.excel_parser import read_to_dataframe
from app.infrastructure.s3_client import S3Client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.mapping.models import MappingRecord
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.repository import escape_cypher_value, format_cypher_value
from app.modules.ontology.schemas import MappingResult
from app.modules.synthesis.models import SynthesisJob
from app.modules.synthesis.schemas import (
    SynthesisJobResponse,
    SynthesisListResponse,
    SynthesisStartRequest,
)
from app.modules.upload.models import Upload
from app.core.exceptions import AppError

router = APIRouter(prefix="/synthesis", tags=["synthesis"])

_s3 = S3Client()

CHUNK_SIZE = 500


# === Cypher 빌더 (schema-per-tenant, _org_id 없음) ===


def _build_merge_node(
    label: str,
    merge_keys: dict[str, str],
    set_props: dict[str, str],
) -> str:
    """노드 MERGE Cypher 생성 (_org_id 없는 schema-per-tenant 버전)."""
    merge_str = ", ".join(f"{k}: {v}" for k, v in merge_keys.items())

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
    """관계 MERGE Cypher 생성 (_org_id 없는 schema-per-tenant 버전)."""
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


# === 행 → Cypher 변환 ===


def _process_row_nodes(
    row: dict,
    mapping: MappingResult,
) -> list[str]:
    """단일 행에서 노드 MERGE Cypher 목록 생성."""
    cyphers: list[str] = []

    # 라벨별 속성 수집
    label_props: dict[str, dict[str, str]] = {}

    # column_mappings 처리
    for cm in mapping.column_mappings:
        val = row.get(cm.source_column)
        formatted = format_cypher_value(val, cm.data_type)
        if formatted is None:
            continue
        label_props.setdefault(cm.target_label, {})[cm.target_property] = formatted

    # extended_properties 처리
    for ep in mapping.extended_properties:
        val = row.get(ep.source_column)
        formatted = format_cypher_value(val, ep.data_type)
        if formatted is None:
            continue
        label_props.setdefault(ep.target_label, {})[ep.property_name] = formatted

    # 라벨별 MERGE Cypher 생성
    for label, props in label_props.items():
        node_def = MANUFACTURING_ONTOLOGY.get_node_label(label)
        if node_def is None:
            continue

        merge_keys: dict[str, str] = {}
        set_props: dict[str, str] = {}

        for mk in node_def.merge_keys:
            if mk in props:
                merge_keys[mk] = props[mk]

        if not merge_keys:
            continue

        for k, v in props.items():
            if k not in merge_keys:
                set_props[k] = v

        cyphers.append(_build_merge_node(label, merge_keys, set_props))

    return cyphers


def _process_row_relationships(
    row: dict,
    mapping: MappingResult,
) -> list[str]:
    """단일 행에서 관계 MERGE Cypher 목록 생성."""
    cyphers: list[str] = []

    # 라벨별 merge key 값 캐시 (관계 생성 시 MATCH 조건에 사용)
    label_merge_vals: dict[str, dict[str, str]] = {}

    for cm in mapping.column_mappings:
        node_def = MANUFACTURING_ONTOLOGY.get_node_label(cm.target_label)
        if node_def is None:
            continue
        if cm.target_property in node_def.merge_keys:
            val = row.get(cm.source_column)
            formatted = format_cypher_value(val, cm.data_type)
            if formatted is not None:
                label_merge_vals.setdefault(cm.target_label, {})[cm.target_property] = (
                    formatted
                )

    for rm in mapping.relation_mappings:
        from_keys = label_merge_vals.get(rm.from_label, {})
        to_keys = label_merge_vals.get(rm.to_label, {})

        if not from_keys or not to_keys:
            continue

        # 관계 속성 수집
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


# === 합성 실행 (BackgroundTask) ===


def _run_synthesis(
    job_id: uuid.UUID,
    schema_name: str,
    graph_name: str,
    file_key: str,
    filename: str,
    header_row: int,
    mapping_json: dict,
) -> None:
    """백그라운드에서 합성 작업 실행.

    별도 DB 세션을 생성하여 작업하며, 청크 단위로 커밋합니다.
    """
    db = SessionLocal()
    try:
        db.execute(text(f"SET search_path = {schema_name}, ag_catalog, public"))

        # 상태: PROCESSING
        job = db.query(SynthesisJob).filter(SynthesisJob.id == job_id).one()
        job.status = "PROCESSING"
        job.started_at = datetime.now(timezone.utc)
        db.commit()

        # S3에서 파일 다운로드 → DataFrame
        content = _s3.get_object(file_key)
        df = read_to_dataframe(content, filename, header_row=header_row)

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

        # 청크 단위 처리
        for chunk_start in range(0, total_rows, CHUNK_SIZE):
            chunk_end = min(chunk_start + CHUNK_SIZE, total_rows)
            chunk = df.iloc[chunk_start:chunk_end]

            for idx, (_, row_series) in enumerate(chunk.iterrows()):
                row_num = chunk_start + idx + 1
                row = row_series.to_dict()

                try:
                    # 노드 MERGE
                    node_cyphers = _process_row_nodes(row, mapping)
                    for cypher in node_cyphers:
                        execute_cypher_raw(db, cypher, graph_name)
                    nodes_created += len(node_cyphers)

                    # 관계 MERGE
                    rel_cyphers = _process_row_relationships(row, mapping)
                    for cypher in rel_cyphers:
                        execute_cypher_raw(db, cypher, graph_name)
                    rels_created += len(rel_cyphers)

                except Exception as e:
                    err_msg = f"행 {row_num}: {e}"
                    errors.append(err_msg)
                    logger.warning("합성 행 처리 오류: {err}", err=err_msg)

                processed += 1

            # 청크 커밋 + 진행률 업데이트
            db.commit()
            job.processed_rows = processed
            job.nodes_created = nodes_created
            job.relationships_created = rels_created
            job.errors = errors[:100]  # 최대 100개 에러만 저장
            db.commit()

            logger.info(
                "합성 진행: job_id={job_id} {processed}/{total}행",
                job_id=job_id,
                processed=processed,
                total=total_rows,
            )

        # 완료
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

    except Exception as e:
        logger.error("합성 실패: job_id={job_id} error={err}", job_id=job_id, err=e)
        try:
            db.rollback()
            job = db.query(SynthesisJob).filter(SynthesisJob.id == job_id).one()
            job.status = "FAILED"
            job.errors = [str(e)]
            job.completed_at = datetime.now(timezone.utc)
            db.commit()
        except Exception:
            logger.error("합성 실패 상태 저장 오류: job_id={job_id}", job_id=job_id)
    finally:
        db.close()


# === API 엔드포인트 ===


@router.post("", response_model=SynthesisJobResponse)
def start_synthesis(
    req: SynthesisStartRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """합성 작업 시작 (백그라운드 실행)."""
    # 1. MappingRecord 조회
    record = db.query(MappingRecord).filter(MappingRecord.id == req.mapping_id).first()
    if record is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")

    # 2. Upload 조회
    upload = db.query(Upload).filter(Upload.id == record.upload_id).first()
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    # 3. SynthesisJob 생성
    job = SynthesisJob(
        id=generate_uuid7(),
        mapping_id=record.id,
        upload_id=upload.id,
        status="PENDING",
    )
    db.add(job)

    # 사용 횟수 증가
    record.usage_count += 1
    db.commit()
    db.refresh(job)

    # 4. 백그라운드 태스크 등록
    schema_name = org_id_to_schema(auth.org_id)
    background_tasks.add_task(
        _run_synthesis,
        job_id=job.id,
        schema_name=schema_name,
        graph_name=schema_name,
        file_key=upload.file_key,
        filename=upload.original_name,
        header_row=req.header_row,
        mapping_json=record.mapping,
    )

    logger.info(
        "합성 작업 시작: job_id={job_id} mapping_id={mapping_id}",
        job_id=job.id,
        mapping_id=record.id,
    )

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


@router.get("/{job_id}", response_model=SynthesisJobResponse)
def get_synthesis_job(
    job_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    """합성 작업 상태 조회."""
    job = db.query(SynthesisJob).filter(SynthesisJob.id == job_id).first()
    if job is None:
        raise AppError(message="합성 작업을 찾을 수 없습니다", code="NOT_FOUND")

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


@router.get("", response_model=SynthesisListResponse)
def list_synthesis_jobs(
    db: Session = Depends(get_tenant_db),
):
    """합성 작업 목록 조회 (최신순)."""
    jobs = db.query(SynthesisJob).order_by(SynthesisJob.created_at.desc()).all()
    return SynthesisListResponse(
        items=[
            SynthesisJobResponse(
                id=j.id,
                mapping_id=j.mapping_id,
                upload_id=j.upload_id,
                status=j.status,
                total_rows=j.total_rows,
                processed_rows=j.processed_rows,
                nodes_created=j.nodes_created,
                relationships_created=j.relationships_created,
                errors=j.errors,
                started_at=j.started_at,
                completed_at=j.completed_at,
                created_at=j.created_at,
            )
            for j in jobs
        ]
    )
