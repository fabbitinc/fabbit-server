"""부품(Part) 도메인 Repository — RDS + Graph 캡슐화.

Service는 저장 위치(RDS/Graph)를 모르고, Repository가 dual-write를 내부적으로 관리합니다.
"""

import re
import uuid

from sqlalchemy import exists, func, select, text
from sqlalchemy.orm import Session

from app.core.database import generate_uuid7
from app.infrastructure.age_client import execute_cypher_raw
from app.modules.drawing.models import Drawing
from app.modules.ontology.cypher_utils import escape_cypher_value
from app.modules.part.models import BomLink, Part, PartRevision, PartSupplier
from app.modules.supplier.models import Supplier

# Part 모델의 표준 속성 (온톨로지 정의 속성 중 RDS 컬럼에 매핑되는 것)
# revision은 별도 관리하므로 제외
_PART_STANDARD_ATTRS = {
    "name",
    "material",
    "unit",
    "description",
    "category",
    "is_phantom",
    "lifecycle_state",
    "lead_time_days",
}

# 리비전 접미사 패턴 (정규식) — 숫자/알파벳 접미사를 탐지하여 자동 증분
_REVISION_SUFFIX_RE = re.compile(r"^(.*?)(\d+|[A-Z])$", re.IGNORECASE)


def next_revision(current: str) -> str:
    """현재 리비전에서 다음 리비전을 자동 생성.

    패턴 감지 규칙:
    - 숫자 접미사: "1" → "2", "Rev.3" → "Rev.4", "003" → "004" (자릿수 유지)
    - 알파벳 접미사: "A" → "B", "Rev.A" → "Rev.B"
    - Z 도달 시: "Z" → "AA" (단독), "Rev.Z" → "Rev.AA"
    """
    m = _REVISION_SUFFIX_RE.match(current)
    if not m:
        # 패턴 감지 실패 → 숫자 1부터 이어붙임
        return current + ".1"

    prefix, suffix = m.group(1), m.group(2)

    if suffix.isdigit():
        # 숫자 접미사: 자릿수 유지하며 +1
        width = len(suffix)
        return prefix + str(int(suffix) + 1).zfill(width)

    # 알파벳 접미사 (단일 문자)
    if suffix.upper() == "Z":
        return prefix + ("AA" if suffix.isupper() else "aa")
    next_char = chr(ord(suffix) + 1)
    return prefix + next_char


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
    category: str | None = None,
    lifecycle_state: str | None = None,
    has_drawing: bool | None = None,
    has_children: bool | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[dict], int]:
    """Part 목록 페이징 조회 (Drawing JOIN + 하위부품 수 포함).

    Returns:
        (items, total) — items는 PartSummary 필드를 포함하는 dict 리스트
    """
    # 필터 조건 조립
    conditions = []
    if search:
        conditions.append(
            Part.part_number.ilike(f"%{search}%") | Part.name.ilike(f"%{search}%")
        )
    if category:
        conditions.append(Part.category == category)
    if lifecycle_state:
        conditions.append(Part.lifecycle_state == lifecycle_state)
    if has_drawing is True:
        conditions.append(Part.drawing_id.isnot(None))
    elif has_drawing is False:
        conditions.append(Part.drawing_id.is_(None))
    if has_children is True:
        conditions.append(exists().where(BomLink.parent_part_id == Part.id))
    elif has_children is False:
        conditions.append(~exists().where(BomLink.parent_part_id == Part.id))

    # 총 건수 (JOIN 없이)
    count_query = db.query(func.count(Part.id))
    for cond in conditions:
        count_query = count_query.filter(cond)
    total = count_query.scalar() or 0

    # 데이터 (Drawing LEFT JOIN + 하위부품 수 서브쿼리)
    children_count_subq = (
        select(func.count(BomLink.id))
        .where(BomLink.parent_part_id == Part.id)
        .correlate(Part)
        .scalar_subquery()
        .label("children_count")
    )

    data_query = (
        db.query(
            Part.id,
            Part.part_number,
            Part.name,
            Part.category,
            Part.revision,
            Part.lifecycle_state,
            Drawing.drawing_number,
            children_count_subq,
        )
        .outerjoin(Drawing, Part.drawing_id == Drawing.id)
    )
    for cond in conditions:
        data_query = data_query.filter(cond)

    rows = data_query.order_by(Part.part_number).offset(offset).limit(limit).all()

    items = [
        {
            "id": r.id,
            "part_number": r.part_number,
            "name": r.name,
            "category": r.category,
            "revision": r.revision,
            "lifecycle_state": r.lifecycle_state,
            "drawing_number": r.drawing_number,
            "children_count": r.children_count or 0,
        }
        for r in rows
    ]
    return items, total


def list_parts_for_export(
    db: Session,
    *,
    search: str | None = None,
    category: str | None = None,
    lifecycle_state: str | None = None,
    has_drawing: bool | None = None,
    has_children: bool | None = None,
    part_ids: list[uuid.UUID] | None = None,
) -> list[Part]:
    """Part 목록 전체 조회 (Excel 내보내기용, pagination 없음, 최대 10,000건)."""
    conditions = []
    if search:
        conditions.append(
            Part.part_number.ilike(f"%{search}%") | Part.name.ilike(f"%{search}%")
        )
    if category:
        conditions.append(Part.category == category)
    if lifecycle_state:
        conditions.append(Part.lifecycle_state == lifecycle_state)
    if has_drawing is True:
        conditions.append(Part.drawing_id.isnot(None))
    elif has_drawing is False:
        conditions.append(Part.drawing_id.is_(None))
    if has_children is True:
        conditions.append(exists().where(BomLink.parent_part_id == Part.id))
    elif has_children is False:
        conditions.append(~exists().where(BomLink.parent_part_id == Part.id))
    if part_ids:
        conditions.append(Part.id.in_(part_ids))

    query = db.query(Part)
    for cond in conditions:
        query = query.filter(cond)

    return query.order_by(Part.part_number).all()


def get_distinct_categories(db: Session) -> list[str]:
    """Part category DISTINCT 값 목록 (NULL 제외)."""
    rows = (
        db.query(Part.category)
        .filter(Part.category.isnot(None))
        .distinct()
        .order_by(Part.category)
        .all()
    )
    return [r[0] for r in rows]


def get_distinct_lifecycle_states(db: Session) -> list[str]:
    """Part lifecycle_state DISTINCT 값 목록 (NULL 제외)."""
    rows = (
        db.query(Part.lifecycle_state)
        .filter(Part.lifecycle_state.isnot(None))
        .distinct()
        .order_by(Part.lifecycle_state)
        .all()
    )
    return [r[0] for r in rows]


def count_all(db: Session) -> int:
    """전체 Part 수 (RDS)."""
    return db.query(func.count(Part.id)).scalar() or 0


def bulk_get_parts(db: Session, part_numbers: list[str]) -> dict[str, dict]:
    """품번 목록에 대한 Part 상세 필드 일괄 조회 (RDS).

    Returns:
        part_number → {id, part_number, name, revision, material, unit, category, lifecycle_state}
    """
    if not part_numbers:
        return {}
    rows = (
        db.query(
            Part.id,
            Part.part_number,
            Part.name,
            Part.revision,
            Part.material,
            Part.unit,
            Part.category,
            Part.lifecycle_state,
        )
        .filter(Part.part_number.in_(part_numbers))
        .all()
    )
    return {
        r.part_number: {
            "id": r.id,
            "part_number": r.part_number,
            "name": r.name,
            "revision": r.revision or "1",
            "material": r.material,
            "unit": r.unit,
            "category": r.category,
            "lifecycle_state": r.lifecycle_state,
        }
        for r in rows
    }


def get_by_part_numbers(db: Session, part_numbers: list[str]) -> list[Part]:
    """품번 목록으로 Part 일괄 조회 (RDS)."""
    return db.query(Part).filter(Part.part_number.in_(part_numbers)).all()


def upsert_part(
    db: Session,
    part_number: str,
    props: dict,
    job_id: uuid.UUID | None,
    graph_name: str,
    *,
    overwrite: bool = False,
) -> None:
    """Part를 RDS에 INSERT/UPDATE하고, Graph에 MERGE.

    RDS: 전체 속성 저장 + PartRevision 기록
    Graph: part_number만 유지 (merge key)

    리비전 관리 (PartRevision = SoT, Part = 최신 비정규화):
    - 신규 Part: incoming revision 사용 (없으면 "1") → PartRevision 첫 기록
    - 기존 Part: 변경 감지 → 적용 → revision 증분 → PartRevision 새 기록
    - revision은 standard 속성과 별도로 관리 (incoming revision은 신규에서만 사용)

    overwrite=False: DB에 이미 값이 있는 필드는 유지 (빈 필드만 채움)
    overwrite=True: 엑셀 값으로 덮어쓰기
    """
    # ── 속성 분류 ──
    standard: dict = {}
    extended: dict = {}
    incoming_revision: str | None = None
    for key, value in props.items():
        if key == "part_number":
            continue
        if key == "revision":
            incoming_revision = str(value) if value is not None else None
            continue
        if key.startswith("_ext_"):
            extended[key] = value
        elif key in _PART_STANDARD_ATTRS:
            standard[key] = value
        else:
            extended[key] = value

    existing = db.query(Part).filter(Part.part_number == part_number).first()

    if existing is None:
        # ── 신규 Part: 생성 + 첫 리비전 기록 ──
        part = Part(
            id=generate_uuid7(),
            part_number=part_number,
            revision=incoming_revision or "1",
            extended_properties=extended if extended else {},
            **standard,
        )
        db.add(part)
        db.flush()
        _create_revision_snapshot(db, part, job_id)
    else:
        # ── 기존 Part: 변경 감지 → 적용 → 증분 → 리비전 기록 ──
        changed = False
        for key, value in standard.items():
            current = getattr(existing, key)
            if not overwrite and current is not None:
                continue
            if current != value:
                changed = True
                break

        if not changed and extended:
            merged_ext = dict(existing.extended_properties or {})
            for key, value in extended.items():
                if not overwrite and merged_ext.get(key) is not None:
                    continue
                if merged_ext.get(key) != value:
                    changed = True
                    break

        if changed:
            # 1) 속성 적용
            for key, value in standard.items():
                current = getattr(existing, key)
                if not overwrite and current is not None:
                    continue
                if current != value:
                    setattr(existing, key, value)

            if extended:
                merged_ext = dict(existing.extended_properties or {})
                for key, value in extended.items():
                    if not overwrite and merged_ext.get(key) is not None:
                        continue
                    merged_ext[key] = value
                existing.extended_properties = merged_ext

            # 2) 리비전 증분
            existing.revision = next_revision(existing.revision)
            db.flush()

            # 3) 변경 후 상태를 새 리비전으로 기록
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
            Part.id,
            Part.part_number,
            Part.name,
            BomLink.quantity,
            BomLink.extended_properties,
        )
        .join(Part, BomLink.child_part_id == Part.id)
        .filter(BomLink.parent_part_id == parent_part_id)
        .order_by(Part.part_number)
        .all()
    )
    return [
        {
            "id": r.id,
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
            Part.id,
            Part.part_number,
            Part.name,
            BomLink.quantity,
            BomLink.extended_properties,
        )
        .join(Part, BomLink.parent_part_id == Part.id)
        .filter(BomLink.child_part_id == child_part_id)
        .order_by(Part.part_number)
        .all()
    )
    return [
        {
            "id": r.id,
            "part_number": r.part_number,
            "name": r.name,
            "quantity": r.quantity or 1,
            "extended_properties": r.extended_properties or {},
        }
        for r in rows
    ]


# ── BOM 트리 (RDS Recursive CTE) ──

# 순환 참조 방지용 최대 탐색 깊이
_MAX_BOM_DEPTH = 50


def get_bom_edges(
    db: Session,
    root_part_id: uuid.UUID,
    *,
    reverse: bool = False,
) -> list[dict]:
    """BOM 간선 목록 조회 (Recursive CTE).

    reverse=False: 정전개 — root의 하위 부품 간선
    reverse=True:  역전개 — root를 사용하는 상위 부품 간선 (quantity=1 고정)

    Returns:
        [{"parent_pn": str, "child_pn": str, "quantity": int}, ...]
        parent_pn은 트리에서 루트에 가까운 쪽, child_pn은 먼 쪽
    """
    if reverse:
        sql = text("""
            WITH RECURSIVE bom_cte(parent_pn, child_pn, quantity, next_id, depth) AS (
                SELECT
                    cp.part_number,
                    pp.part_number,
                    1,
                    bl.parent_part_id,
                    1
                FROM bom_links bl
                JOIN parts cp ON cp.id = bl.child_part_id
                JOIN parts pp ON pp.id = bl.parent_part_id
                WHERE bl.child_part_id = :root_id

                UNION ALL

                SELECT
                    cp.part_number,
                    pp.part_number,
                    1,
                    bl.parent_part_id,
                    bc.depth + 1
                FROM bom_cte bc
                JOIN bom_links bl ON bl.child_part_id = bc.next_id
                JOIN parts cp ON cp.id = bl.child_part_id
                JOIN parts pp ON pp.id = bl.parent_part_id
                WHERE bc.depth < :max_depth
            )
            SELECT parent_pn, child_pn, quantity FROM bom_cte
        """)
    else:
        sql = text("""
            WITH RECURSIVE bom_cte(parent_pn, child_pn, quantity, next_id, depth) AS (
                SELECT
                    pp.part_number,
                    cp.part_number,
                    bl.quantity,
                    bl.child_part_id,
                    1
                FROM bom_links bl
                JOIN parts pp ON pp.id = bl.parent_part_id
                JOIN parts cp ON cp.id = bl.child_part_id
                WHERE bl.parent_part_id = :root_id

                UNION ALL

                SELECT
                    pp.part_number,
                    cp.part_number,
                    bl.quantity,
                    bl.child_part_id,
                    bc.depth + 1
                FROM bom_cte bc
                JOIN bom_links bl ON bl.parent_part_id = bc.next_id
                JOIN parts pp ON pp.id = bl.parent_part_id
                JOIN parts cp ON cp.id = bl.child_part_id
                WHERE bc.depth < :max_depth
            )
            SELECT parent_pn, child_pn, quantity FROM bom_cte
        """)

    rows = db.execute(sql, {"root_id": root_part_id, "max_depth": _MAX_BOM_DEPTH}).fetchall()
    return [
        {"parent_pn": r.parent_pn, "child_pn": r.child_pn, "quantity": r.quantity}
        for r in rows
    ]


# ── 비-BOM 관계 (RDS + Graph dual-write) ──


def get_drawing(db: Session, part_id: uuid.UUID) -> dict | None:
    """Part.drawing_id FK로 연결된 Drawing 단건 조회 (RDS)."""
    part = db.query(Part).filter(Part.id == part_id).first()
    if not part or not part.drawing_id:
        return None
    drawing = db.query(Drawing).filter(Drawing.id == part.drawing_id).first()
    if not drawing:
        return None
    return {
        "id": drawing.id,
        "drawing_number": drawing.drawing_number,
        "name": drawing.name,
        "version": drawing.version,
        "status": drawing.status,
        "conversion_status": drawing.conversion_status,
        "original_file_key": drawing.original_file_key,
        "pdf_key": drawing.pdf_key,
        "thumbnail_key": drawing.thumbnail_key,
    }


def get_suppliers(db: Session, part_id: uuid.UUID) -> list[dict]:
    """PartSupplier 조인 테이블로 공급사 조회 (RDS)."""
    rows = (
        db.query(
            Supplier.id,
            Supplier.company_name,
            Supplier.code,
            Supplier.country,
            PartSupplier.unit_cost,
            PartSupplier.extended_properties,
        )
        .join(Supplier, PartSupplier.supplier_id == Supplier.id)
        .filter(PartSupplier.part_id == part_id)
        .order_by(Supplier.company_name)
        .all()
    )
    return [
        {
            "id": r.id,
            "company_name": r.company_name,
            "code": r.code,
            "country": r.country,
            "unit_cost": r.unit_cost,
            "extended_properties": r.extended_properties or {},
        }
        for r in rows
    ]


def link_part_to_drawing(
    db: Session,
    graph_name: str,
    part_number: str,
    drawing_number: str,
) -> None:
    """Part.drawing_id 설정 + Graph DEFINED_BY MERGE."""
    part = db.query(Part).filter(Part.part_number == part_number).first()
    drawing = (
        db.query(Drawing).filter(Drawing.drawing_number == drawing_number).first()
    )
    if not part or not drawing:
        return

    part.drawing_id = drawing.id
    db.flush()

    # Graph MERGE
    esc_pn = escape_cypher_value(part_number)
    esc_dn = escape_cypher_value(drawing_number)
    cypher = (
        f"MATCH (p:Part {{part_number: '{esc_pn}'}}), "
        f"(d:Drawing {{drawing_number: '{esc_dn}'}}) "
        f"MERGE (p)-[:DEFINED_BY]->(d)"
    )
    execute_cypher_raw(db, cypher, graph_name)


def link_part_to_supplier(
    db: Session,
    graph_name: str,
    part_number: str,
    company_name: str,
    *,
    unit_cost: float | None = None,
    extended_properties: dict | None = None,
    overwrite: bool = False,
) -> None:
    """PartSupplier 생성/갱신 + Graph SUPPLIED_BY MERGE."""
    part = db.query(Part).filter(Part.part_number == part_number).first()
    supplier = (
        db.query(Supplier).filter(Supplier.company_name == company_name).first()
    )
    if not part or not supplier:
        return

    # RDS: upsert
    existing = (
        db.query(PartSupplier)
        .filter(
            PartSupplier.part_id == part.id,
            PartSupplier.supplier_id == supplier.id,
        )
        .first()
    )

    if existing:
        if overwrite and unit_cost is not None:
            existing.unit_cost = unit_cost
        elif existing.unit_cost is None and unit_cost is not None:
            existing.unit_cost = unit_cost

        if extended_properties:
            merged = dict(existing.extended_properties or {})
            for key, value in extended_properties.items():
                if overwrite or merged.get(key) is None:
                    merged[key] = value
            existing.extended_properties = merged
    else:
        link = PartSupplier(
            part_id=part.id,
            supplier_id=supplier.id,
            unit_cost=unit_cost,
            extended_properties=extended_properties or {},
        )
        db.add(link)

    db.flush()

    # Graph: MERGE SUPPLIED_BY
    esc_pn = escape_cypher_value(part_number)
    esc_cn = escape_cypher_value(company_name)
    if unit_cost is not None:
        cypher = (
            f"MATCH (p:Part {{part_number: '{esc_pn}'}}), "
            f"(s:Supplier {{company_name: '{esc_cn}'}}) "
            f"MERGE (p)-[r:SUPPLIED_BY]->(s) "
            f"SET r.unit_cost = {float(unit_cost)}"
        )
    else:
        cypher = (
            f"MATCH (p:Part {{part_number: '{esc_pn}'}}), "
            f"(s:Supplier {{company_name: '{esc_cn}'}}) "
            f"MERGE (p)-[:SUPPLIED_BY]->(s)"
        )
    execute_cypher_raw(db, cypher, graph_name)


# ── 내부 헬퍼 ──


def search_merge_key(
    db: Session,
    search: str,
    limit: int = 10,
) -> list[dict]:
    """root_context 자동완성용 merge key 검색 (part_number OR name, label=name)."""
    query = db.query(Part.part_number, Part.name).filter(
        Part.part_number.ilike(f"%{search}%") | Part.name.ilike(f"%{search}%")
    )
    rows = query.order_by(Part.part_number).limit(limit).all()
    return [{"value": r.part_number, "label": r.name} for r in rows]


def _create_revision_snapshot(
    db: Session, part: Part, job_id: uuid.UUID | None
) -> None:
    """Part의 현재 상태를 PartRevision으로 아카이브 (변경 전 스냅샷)."""
    revision = PartRevision(
        id=generate_uuid7(),
        part_id=part.id,
        synthesis_job_id=job_id,
        drawing_id=part.drawing_id,
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
