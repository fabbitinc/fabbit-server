"""도면 분석 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.core.database import generate_uuid7
from app.infrastructure.age_client import execute_cypher, execute_cypher_raw
from app.modules.drawing.models import Drawing, DrawingAnalysisRecord, DrawingSynthesisJob
from app.modules.ontology.cypher_utils import escape_cypher_value
from app.modules.file.models import File

# Drawing 모델의 표준 속성 (온톨로지 정의 속성 중 RDS 컬럼에 매핑되는 것)
# 온톨로지의 file_path → RDS의 file_key로 매핑
_DRAWING_STANDARD_ATTRS = {"name", "file_path", "version", "status"}


def get_file_by_id(db: Session, file_id: uuid.UUID) -> File | None:
    return db.query(File).filter(File.id == file_id).first()


def get_drawing_by_id(db: Session, drawing_id: uuid.UUID) -> Drawing | None:
    """Drawing PK 조회."""
    return db.query(Drawing).filter(Drawing.id == drawing_id).first()


def get_drawing_by_original_file_id(db: Session, file_id: uuid.UUID) -> Drawing | None:
    """원본 파일 ID로 Drawing 조회."""
    return db.query(Drawing).filter(Drawing.original_file_id == file_id).first()


# ── DrawingAnalysisRecord CRUD ──


def create_analysis_record(
    db: Session,
    record_id: uuid.UUID,
    file_id: uuid.UUID,
    name: str,
    analysis: dict,
    page_count: int,
) -> DrawingAnalysisRecord:
    record = DrawingAnalysisRecord(
        id=record_id,
        file_id=file_id,
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


# ── Drawing RDS + Graph dual-write ──


def upsert_drawing(
    db: Session,
    drawing_number: str | None,
    props: dict,
    graph_name: str,
    *,
    overwrite: bool = False,
    original_file_id: uuid.UUID | None = None,
) -> None:
    """Drawing을 RDS에 INSERT/UPDATE하고, Graph에 MERGE.

    RDS: 전체 속성 저장
    Graph: drawing_number가 있을 때만 MERGE (merge key)

    검색 순서: (1) original_file_id로 기존 Drawing 검색, (2) drawing_number로 검색.
    original_file_id로 찾은 경우 drawing_number도 업데이트.

    overwrite=False: DB에 이미 값이 있는 필드는 유지 (빈 필드만 채움)
    overwrite=True: 엑셀 값으로 덮어쓰기
    """
    # ── RDS upsert ──
    standard: dict = {}
    extended: dict = {}
    for key, value in props.items():
        if key == "drawing_number":
            continue
        if key.startswith("_ext_"):
            extended[key] = value
        elif key in _DRAWING_STANDARD_ATTRS:
            standard[key] = value
        else:
            extended[key] = value

    # file_path → file_key 매핑
    if "file_path" in standard:
        standard["file_key"] = standard.pop("file_path")

    # 기존 Drawing 검색: original_file_id 우선, 없으면 drawing_number
    existing = None
    if original_file_id:
        existing = (
            db.query(Drawing)
            .filter(Drawing.original_file_id == original_file_id)
            .first()
        )
    if existing is None and drawing_number:
        existing = (
            db.query(Drawing)
            .filter(Drawing.drawing_number == drawing_number)
            .first()
        )

    if existing is None:
        drawing = Drawing(
            id=generate_uuid7(),
            drawing_number=drawing_number,
            original_file_id=original_file_id,
            # name은 필수 컬럼이므로 기본값 제공
            name=standard.pop("name", drawing_number or "Untitled"),
            extended_properties=extended if extended else {},
            **standard,
        )
        db.add(drawing)
        db.flush()
    else:
        changed = False
        # drawing_number 업데이트 (None → 값, 값 변경 모두 포함)
        if drawing_number and existing.drawing_number != drawing_number:
            existing.drawing_number = drawing_number
            changed = True
        # original_file_id가 비어 있으면 채움
        if original_file_id and existing.original_file_id is None:
            existing.original_file_id = original_file_id
            changed = True
        for key, value in standard.items():
            current = getattr(existing, key)
            if not overwrite and current is not None:
                continue
            if current != value:
                setattr(existing, key, value)
                changed = True

        if extended:
            merged_ext = dict(existing.extended_properties or {})
            for key, value in extended.items():
                if not overwrite and merged_ext.get(key) is not None:
                    continue
                if merged_ext.get(key) != value:
                    merged_ext[key] = value
                    changed = True
            if changed:
                existing.extended_properties = merged_ext

        if changed:
            db.flush()

    # ── Graph MERGE (drawing_number가 있을 때만) ──
    if drawing_number:
        escaped = escape_cypher_value(drawing_number)
        cypher = f"MERGE (n:Drawing {{drawing_number: '{escaped}'}})"
        execute_cypher_raw(db, cypher, graph_name)


def list_drawings_paginated(
    db: Session,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[Drawing], int]:
    """Drawing 목록 페이징 조회 (RDS)."""
    query = db.query(Drawing)
    if search:
        query = query.filter(
            Drawing.drawing_number.ilike(f"%{search}%")
            | Drawing.name.ilike(f"%{search}%")
        )
    total = query.count()
    drawings = (
        query.order_by(Drawing.drawing_number).offset(offset).limit(limit).all()
    )
    return drawings, total


def search_merge_key(
    db: Session,
    search: str,
    limit: int = 10,
) -> list[dict]:
    """root_context 자동완성용 merge key 검색 (drawing_number OR name, label=name)."""
    query = db.query(Drawing.drawing_number, Drawing.name).filter(
        Drawing.drawing_number.isnot(None),
        Drawing.drawing_number.ilike(f"%{search}%")
        | Drawing.name.ilike(f"%{search}%"),
    )
    rows = query.order_by(Drawing.drawing_number).limit(limit).all()
    return [{"value": r.drawing_number, "label": r.name} for r in rows]
