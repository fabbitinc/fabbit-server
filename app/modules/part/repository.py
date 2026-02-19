"""부품(Part) 도메인 Repository — RDS + Graph 캡슐화.

Service는 저장 위치(RDS/Graph)를 모르고, Repository가 dual-write를 내부적으로 관리합니다.
"""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.database import generate_uuid7
from app.infrastructure.age_client import execute_cypher, execute_cypher_raw
from app.modules.ontology.cypher_utils import escape_cypher_value
from app.modules.part.models import BomLink, Part, PartRevision

# Part 모델의 표준 속성 (온톨로지 정의 속성 중 RDS 컬럼에 매핑되는 것)
_PART_STANDARD_ATTRS = {
    "name",
    "revision",
    "material",
    "unit",
    "description",
    "category",
    "is_phantom",
    "lifecycle_state",
    "lead_time_days",
}


class MissingPartForBomError(Exception):
    """BOM 링크 생성에 필요한 Part가 없는 경우."""

    def __init__(self, parent_pn: str, child_pn: str) -> None:
        self.parent_pn = parent_pn
        self.child_pn = child_pn
        super().__init__(
            f"BOM 링크 대상 Part가 없습니다 (parent={parent_pn}, child={child_pn})"
        )


# ── Part CRUD ──


def get_by_part_number(db: Session, part_number: str) -> Part | None:
    """품번으로 Part 조회 (RDS)."""
    return db.query(Part).filter(Part.part_number == part_number).first()


def get_by_id(db: Session, part_id: uuid.UUID) -> Part | None:
    """Part ID로 조회 (RDS)."""
    return db.query(Part).filter(Part.id == part_id).first()


def list_parts_paginated(
    db: Session,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[Part], int]:
    """Part 목록 페이징 조회 (RDS)."""
    query = db.query(Part)
    if search:
        query = query.filter(
            Part.part_number.ilike(f"%{search}%") | Part.name.ilike(f"%{search}%")
        )
    total = query.count()
    parts = query.order_by(Part.part_number).offset(offset).limit(limit).all()
    return parts, total


def count_all(db: Session) -> int:
    """전체 Part 수 (RDS)."""
    return db.query(func.count(Part.id)).scalar() or 0


def bulk_get_names(db: Session, part_numbers: list[str]) -> dict[str, str | None]:
    """품번 목록에 대한 이름 일괄 조회 (RDS)."""
    if not part_numbers:
        return {}
    rows = (
        db.query(Part.part_number, Part.name)
        .filter(Part.part_number.in_(part_numbers))
        .all()
    )
    return {pn: name for pn, name in rows}


def get_by_part_numbers(db: Session, part_numbers: list[str]) -> list[Part]:
    """품번 목록으로 Part 일괄 조회 (RDS)."""
    return db.query(Part).filter(Part.part_number.in_(part_numbers)).all()


def upsert_part(
    db: Session,
    part_number: str,
    props: dict,
    job_id: uuid.UUID,
    graph_name: str,
    *,
    overwrite: bool = False,
) -> None:
    """Part를 RDS에 INSERT/UPDATE하고, Graph에 MERGE.

    RDS: 전체 속성 저장 + PartRevision 스냅샷
    Graph: part_number만 유지 (merge key)

    overwrite=False: DB에 이미 값이 있는 필드는 유지 (빈 필드만 채움)
    overwrite=True: 엑셀 값으로 덮어쓰기
    """
    # ── RDS upsert ──
    standard: dict = {}
    extended: dict = {}
    for key, value in props.items():
        if key == "part_number":
            continue
        if key.startswith("_ext_"):
            extended[key] = value
        elif key in _PART_STANDARD_ATTRS:
            standard[key] = value
        else:
            # 온톨로지에 정의되었지만 Part 컬럼에 없는 속성 → 확장 속성으로 저장
            extended[key] = value

    existing = db.query(Part).filter(Part.part_number == part_number).first()

    if existing is None:
        part = Part(
            id=generate_uuid7(),
            part_number=part_number,
            extended_properties=extended if extended else {},
            **standard,
        )
        db.add(part)
        db.flush()
        _create_revision_snapshot(db, part, job_id)
    else:
        changed = False
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
            _create_revision_snapshot(db, existing, job_id)

    # ── Graph MERGE (part_number만) ──
    escaped = escape_cypher_value(part_number)
    cypher = f"MERGE (n:Part {{part_number: '{escaped}'}})"
    execute_cypher_raw(db, cypher, graph_name)


# ── BOM 관계 (RDS + Graph dual-write) ──


def upsert_bom_link(
    db: Session,
    graph_name: str,
    parent_pn: str,
    child_pn: str,
    quantity: int = 1,
    *,
    extended_properties: dict | None = None,
    overwrite: bool = False,
) -> None:
    """BOM 관계를 RDS(bom_links)와 Graph(CONSISTS_OF)에 동시 저장.

    overwrite=False: 기존 값 유지, 빈 필드만 엑셀 값으로 채움
    overwrite=True: 엑셀 값으로 덮어쓰기 (엑셀에 없는 기존 확장 속성은 유지)
    """
    # ── RDS ──
    parent = db.query(Part).filter(Part.part_number == parent_pn).first()
    child = db.query(Part).filter(Part.part_number == child_pn).first()
    if not parent or not child:
        raise MissingPartForBomError(parent_pn=parent_pn, child_pn=child_pn)

    existing = (
        db.query(BomLink)
        .filter(
            BomLink.parent_part_id == parent.id,
            BomLink.child_part_id == child.id,
        )
        .first()
    )

    if existing:
        if overwrite:
            existing.quantity = quantity
        if extended_properties:
            merged = dict(existing.extended_properties or {})
            for key, value in extended_properties.items():
                if overwrite or merged.get(key) is None:
                    merged[key] = value
            existing.extended_properties = merged
    else:
        link = BomLink(
            parent_part_id=parent.id,
            child_part_id=child.id,
            quantity=quantity,
            extended_properties=extended_properties or {},
        )
        db.add(link)

    db.flush()

    # ── Graph: MERGE CONSISTS_OF ──
    esc_parent = escape_cypher_value(parent_pn)
    esc_child = escape_cypher_value(child_pn)
    set_parts = [f"r.quantity = {int(quantity)}"]
    for ext_key, ext_val in (extended_properties or {}).items():
        esc_key = escape_cypher_value(ext_key)
        if isinstance(ext_val, (int, float)):
            set_parts.append(f"r.`{esc_key}` = {ext_val}")
        elif isinstance(ext_val, list):
            # 배열 값 (예: reference_designator) → 문자열로 직렬화
            esc_val = escape_cypher_value(str(ext_val))
            set_parts.append(f"r.`{esc_key}` = '{esc_val}'")
        else:
            esc_val = escape_cypher_value(str(ext_val))
            set_parts.append(f"r.`{esc_key}` = '{esc_val}'")

    set_str = ", ".join(set_parts)
    cypher = (
        f"MATCH (a:Part {{part_number: '{esc_parent}'}}), "
        f"(b:Part {{part_number: '{esc_child}'}}) "
        f"MERGE (a)-[r:CONSISTS_OF]->(b) "
        f"SET {set_str}"
    )
    execute_cypher_raw(db, cypher, graph_name)


# ── BOM 조회 (RDS) ──


def get_children(db: Session, parent_part_id: uuid.UUID) -> list[dict]:
    """부모 Part의 CONSISTS_OF 자식 목록 (depth 1, RDS JOIN)."""
    rows = (
        db.query(
            BomLink.quantity,
            BomLink.extended_properties,
            Part.part_number,
            Part.name,
        )
        .join(Part, BomLink.child_part_id == Part.id)
        .filter(BomLink.parent_part_id == parent_part_id)
        .order_by(Part.part_number)
        .all()
    )
    return [
        {
            "part_number": r.part_number,
            "name": r.name,
            "quantity": r.quantity or 1,
            "extended_properties": r.extended_properties or {},
        }
        for r in rows
    ]


def get_parents(db: Session, child_part_id: uuid.UUID) -> list[dict]:
    """자식 Part의 CONSISTS_OF 부모 목록 (depth 1, RDS JOIN)."""
    rows = (
        db.query(
            BomLink.quantity,
            BomLink.extended_properties,
            Part.part_number,
            Part.name,
        )
        .join(Part, BomLink.parent_part_id == Part.id)
        .filter(BomLink.child_part_id == child_part_id)
        .order_by(Part.part_number)
        .all()
    )
    return [
        {
            "part_number": r.part_number,
            "name": r.name,
            "quantity": r.quantity or 1,
            "extended_properties": r.extended_properties or {},
        }
        for r in rows
    ]


# ── BOM 트리 (Graph — 다단계 탐색) ──


def get_bom_paths(db: Session, part_number: str, graph_name: str) -> list[dict]:
    """BOM 전체 경로 (가변 길이 CONSISTS_OF).

    AGE는 list comprehension 내 property 접근(n.prop)을 지원하지 않으므로
    nodes(path)/relationships(path)를 반환 후 Python에서 파싱합니다.
    """
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH path = (root:Part {{part_number: '{escaped}'}})"
        f"-[:CONSISTS_OF*]->(child:Part) "
        f"RETURN nodes(path), relationships(path)"
    )
    raw = execute_cypher(db, query, graph_name)
    results = []
    for row in raw:
        nodes = row["c0"] if isinstance(row, dict) else row[0]
        rels = row["c1"] if isinstance(row, dict) else row[1]
        pn_list = []
        for v in nodes or []:
            props = v.get("properties", {}) if isinstance(v, dict) else {}
            pn_list.append(props.get("part_number"))
        qty_list = []
        ref_list = []
        for e in rels or []:
            props = e.get("properties", {}) if isinstance(e, dict) else {}
            qty_list.append(props.get("quantity"))
            ref_list.append(props.get("reference_designator"))
        results.append({"c0": pn_list, "c1": qty_list, "c2": ref_list})
    return results


# ── 비-BOM 관계 (Graph only — 향후 drawing/, supplier/ 모듈로 분리 예정) ──


def get_drawings(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """DEFINED_BY 도면 (Graph)."""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (p:Part {{part_number: '{escaped}'}})-[:DEFINED_BY]->(d:Drawing) "
        f"RETURN d.drawing_number, d.name, d.version, d.status "
        f"ORDER BY d.drawing_number"
    )
    return execute_cypher(db, query, graph_name)


def get_suppliers(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """SUPPLIED_BY 공급사 (Graph)."""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (p:Part {{part_number: '{escaped}'}})-[r:SUPPLIED_BY]->(s:Supplier) "
        f"RETURN s.company_name, s.code, s.country, r.unit_cost "
        f"ORDER BY s.company_name"
    )
    return execute_cypher(db, query, graph_name)


# ── 내부 헬퍼 ──


def _create_revision_snapshot(db: Session, part: Part, job_id: uuid.UUID) -> None:
    """Part의 현재 상태를 PartRevision으로 스냅샷."""
    revision = PartRevision(
        id=generate_uuid7(),
        part_id=part.id,
        synthesis_job_id=job_id,
        part_number=part.part_number,
        name=part.name,
        revision=part.revision,
        material=part.material,
        unit=part.unit,
        description=part.description,
        category=part.category,
        is_phantom=part.is_phantom,
        lifecycle_state=part.lifecycle_state,
        lead_time_days=part.lead_time_days,
        extended_properties=dict(part.extended_properties or {}),
    )
    db.add(revision)
