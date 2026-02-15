"""도면 분석 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher, execute_cypher_raw
from app.modules.drawing.models import DrawingAnalysisRecord, DrawingSynthesisJob
from app.modules.ontology.cypher_utils import escape_cypher_value
from app.modules.upload.models import Upload


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


# ── AGE 그래프 쓰기 ──


def merge_drawing_node(
    db: Session,
    graph_name: str,
    drawing_number: str,
    set_props: dict[str, str] | None = None,
) -> None:
    """Drawing 노드 MERGE (Graph only)."""
    esc_dn = escape_cypher_value(drawing_number)
    cypher = f"MERGE (d:Drawing {{drawing_number: '{esc_dn}'}})"
    if set_props:
        set_parts = [f"d.{k} = {v}" for k, v in set_props.items()]
        cypher += " SET " + ", ".join(set_parts)
    execute_cypher_raw(db, cypher, graph_name)


def merge_part_node(
    db: Session,
    graph_name: str,
    part_number: str,
    set_props: dict[str, str] | None = None,
) -> None:
    """Part 노드 MERGE (Graph only)."""
    esc_pn = escape_cypher_value(part_number)
    cypher = f"MERGE (p:Part {{part_number: '{esc_pn}'}})"
    if set_props:
        set_parts = [f"p.{k} = {v}" for k, v in set_props.items()]
        cypher += " SET " + ", ".join(set_parts)
    execute_cypher_raw(db, cypher, graph_name)


def merge_defined_by(
    db: Session,
    graph_name: str,
    part_number: str,
    drawing_number: str,
) -> None:
    """Part → Drawing DEFINED_BY 관계 MERGE (Graph only)."""
    esc_pn = escape_cypher_value(part_number)
    esc_dn = escape_cypher_value(drawing_number)
    cypher = (
        f"MATCH (p:Part {{part_number: '{esc_pn}'}}), "
        f"(d:Drawing {{drawing_number: '{esc_dn}'}}) "
        f"MERGE (p)-[:DEFINED_BY]->(d)"
    )
    execute_cypher_raw(db, cypher, graph_name)
