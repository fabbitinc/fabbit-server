"""도면 분석 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher
from app.modules.drawing.models import DrawingAnalysisRecord, DrawingSynthesisJob
from app.modules.upload.models import Upload


def set_search_path(db: Session, schema_name: str) -> None:
    db.execute(text(f"SET search_path = {schema_name}, ag_catalog, public"))


def get_upload_by_id(db: Session, upload_id: uuid.UUID) -> Upload | None:
    return db.query(Upload).filter(Upload.id == upload_id).first()


# ── DrawingAnalysisRecord CRUD ──


def create_analysis_record(
    db: Session,
    record_id: uuid.UUID,
    upload_id: uuid.UUID,
    name: str,
    analysis: dict,
    page_count: int,
) -> DrawingAnalysisRecord:
    record = DrawingAnalysisRecord(
        id=record_id,
        upload_id=upload_id,
        name=name,
        analysis=analysis,
        page_count=page_count,
    )
    db.add(record)
    return record


def get_analysis_by_id(
    db: Session, analysis_id: uuid.UUID
) -> DrawingAnalysisRecord | None:
    return (
        db.query(DrawingAnalysisRecord)
        .filter(DrawingAnalysisRecord.id == analysis_id)
        .first()
    )


def list_analysis_records(db: Session) -> list[DrawingAnalysisRecord]:
    return (
        db.query(DrawingAnalysisRecord)
        .order_by(DrawingAnalysisRecord.created_at.desc())
        .all()
    )


# ── DrawingSynthesisJob CRUD ──


def create_synthesis_job(
    db: Session,
    job_id: uuid.UUID,
    analysis_id: uuid.UUID,
) -> DrawingSynthesisJob:
    job = DrawingSynthesisJob(
        id=job_id,
        analysis_id=analysis_id,
        status="PENDING",
    )
    db.add(job)
    return job


def get_synthesis_job_by_id(
    db: Session, job_id: uuid.UUID
) -> DrawingSynthesisJob | None:
    return (
        db.query(DrawingSynthesisJob)
        .filter(DrawingSynthesisJob.id == job_id)
        .first()
    )


def get_synthesis_job_required(
    db: Session, job_id: uuid.UUID
) -> DrawingSynthesisJob:
    return (
        db.query(DrawingSynthesisJob)
        .filter(DrawingSynthesisJob.id == job_id)
        .one()
    )


# ── AGE 그래프 조회 ──


def find_existing_parts_by_numbers(
    db: Session,
    graph_name: str,
    part_numbers: list[str],
) -> dict[str, dict]:
    """part_number 리스트로 기존 Part 노드 조회.

    Returns:
        {part_number: {"name": ..., "material": ...}} 딕셔너리
    """
    if not part_numbers:
        return {}

    # Cypher IN 절용 리스트 생성
    escaped = [f"'{pn.replace(chr(39), chr(39)+chr(39))}'" for pn in part_numbers]
    in_list = ", ".join(escaped)

    query = f"MATCH (p:Part) WHERE p.part_number IN [{in_list}] RETURN p.part_number, p.name"
    rows = execute_cypher(db, query, graph_name)

    result: dict[str, dict] = {}
    for row in rows:
        pn = row.get("c0") if isinstance(row, dict) else row
        name = row.get("c1") if isinstance(row, dict) else None
        if pn:
            result[str(pn)] = {"name": str(name) if name else None}
    return result
