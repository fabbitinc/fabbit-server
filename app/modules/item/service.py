"""아이템(Part) 조회 비즈니스 로직.

속성과 BOM 관계는 RDS에서, 비-BOM 관계(Drawing, Supplier)는 Graph에서 읽습니다.
"""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.item import repository as repo
from app.modules.part import repository as part_repo
from app.modules.item.schemas import (
    BomChild,
    BomParent,
    BomTreeNode,
    BomTreeResponse,
    PartDetailResponse,
    PartListResponse,
    PartSummary,
    RelatedDrawing,
    RelatedSupplier,
)
from app.modules.part.models import Part


def _safe_int(val, default: int = 0) -> int:
    """agtype에서 파싱된 값을 int로 변환"""
    if val is None:
        return default
    try:
        return int(val)
    except (ValueError, TypeError):
        return default


def _safe_float(val) -> float | None:
    if val is None:
        return None
    try:
        return float(val)
    except (ValueError, TypeError):
        return None


def _safe_str(val) -> str | None:
    if val is None:
        return None
    s = str(val).strip()
    return s if s else None


# ── Part 목록 ──


def list_items(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> PartListResponse:
    query = db.query(Part)
    if search:
        query = query.filter(
            Part.part_number.ilike(f"%{search}%")
            | Part.name.ilike(f"%{search}%")
        )
    total = query.count()
    parts = query.order_by(Part.part_number).offset(offset).limit(limit).all()

    items = [
        PartSummary(
            id=p.id,
            part_number=p.part_number,
            name=p.name,
            category=p.category,
            lifecycle_state=p.lifecycle_state,
        )
        for p in parts
    ]

    return PartListResponse(total=total, offset=offset, limit=limit, items=items)


# ── Part 상세 ──


def get_item(db: Session, auth: AuthContext, part_number: str) -> PartDetailResponse:
    graph_name = org_id_to_schema(auth.org_id)

    # 속성: RDS
    part = db.query(Part).filter(Part.part_number == part_number).first()
    if not part:
        raise AppError(message=f"Part '{part_number}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    # BOM 관계: RDS JOIN (name 포함)
    children_rows = part_repo.get_children(db, part.id)
    parents_rows = part_repo.get_parents(db, part.id)

    # 비-BOM 관계: Graph
    drawings_rows = repo.get_drawings(db, graph_name, part_number)
    suppliers_rows = repo.get_suppliers(db, graph_name, part_number)

    children = [
        BomChild(
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            sequence=r["sequence"],
            reference_designator=r["reference_designator"],
            find_number=r["find_number"],
        )
        for r in children_rows
    ]

    parents = [
        BomParent(
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            sequence=r["sequence"],
            reference_designator=r["reference_designator"],
            find_number=r["find_number"],
        )
        for r in parents_rows
    ]

    drawings = [
        RelatedDrawing(
            drawing_number=r["c0"] or "",
            name=_safe_str(r["c1"]),
            version=_safe_str(r["c2"]),
            status=_safe_str(r["c3"]),
        )
        for r in drawings_rows
    ]

    suppliers = [
        RelatedSupplier(
            company_name=r["c0"] or "",
            code=_safe_str(r["c1"]),
            country=_safe_str(r["c2"]),
            unit_cost=_safe_float(r["c3"]),
        )
        for r in suppliers_rows
    ]

    extended = {k: v for k, v in (part.extended_properties or {}).items() if v is not None}

    return PartDetailResponse(
        id=part.id,
        part_number=part.part_number,
        name=part.name,
        revision=part.revision,
        material=part.material,
        unit=part.unit,
        description=part.description,
        category=part.category,
        lifecycle_state=part.lifecycle_state,
        is_phantom=part.is_phantom,
        lead_time_days=part.lead_time_days,
        extended_properties=extended,
        children=children,
        parents=parents,
        drawings=drawings,
        suppliers=suppliers,
    )


# ── BOM 트리 ──


def _build_bom_tree(
    root_pn: str,
    root_name: str | None,
    paths: list[dict],
    name_map: dict[str, str | None],
) -> BomTreeNode:
    """경로 리스트를 트리 구조로 조립.

    각 path row는:
      c0: [root_pn, child1_pn, child2_pn, ...]  (nodes의 part_number)
      c1: [qty1, qty2, ...]                       (relationships의 quantity)
      c2: [ref1, ref2, ...]                       (relationships의 reference_designator)
    """
    node_cache: dict[str, BomTreeNode] = {}
    root = BomTreeNode(part_number=root_pn, name=root_name)
    node_cache[root_pn] = root

    for row in paths:
        pn_path = row["c0"] or []
        qty_path = row["c1"] or []
        ref_path = row["c2"] or []

        for i in range(len(pn_path) - 1):
            parent_pn = pn_path[i]
            child_pn = pn_path[i + 1]
            qty = _safe_int(qty_path[i] if i < len(qty_path) else None, 1)
            ref = _safe_str(ref_path[i] if i < len(ref_path) else None)

            if parent_pn not in node_cache:
                node_cache[parent_pn] = BomTreeNode(
                    part_number=parent_pn, name=name_map.get(parent_pn)
                )

            parent_node = node_cache[parent_pn]

            child_key = f"{parent_pn}->{child_pn}"
            if child_key not in node_cache:
                child_node = BomTreeNode(
                    part_number=child_pn,
                    name=name_map.get(child_pn),
                    quantity=qty,
                    reference_designator=ref,
                )
                node_cache[child_key] = child_node
                parent_node.children.append(child_node)

    return root


def get_item_bom_tree(
    db: Session,
    auth: AuthContext,
    part_number: str,
) -> BomTreeResponse:
    graph_name = org_id_to_schema(auth.org_id)

    # 루트 Part 존재 확인 (RDS)
    part = db.query(Part).filter(Part.part_number == part_number).first()
    if not part:
        raise AppError(message=f"Part '{part_number}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    paths = part_repo.get_bom_paths(db, part_number, graph_name)

    # paths에서 모든 part_number 추출하여 name 일괄 조회
    all_pns: set[str] = {part_number}
    for row in paths:
        for pn in (row["c0"] or []):
            if pn:
                all_pns.add(pn)
    name_map = _bulk_get_names(db, list(all_pns))

    root = _build_bom_tree(
        root_pn=part_number,
        root_name=part.name,
        paths=paths,
        name_map=name_map,
    )

    return BomTreeResponse(root=root)


def _bulk_get_names(db: Session, part_numbers: list[str]) -> dict[str, str | None]:
    """part_number 목록에 대한 name을 RDS에서 일괄 조회."""
    if not part_numbers:
        return {}
    rows = (
        db.query(Part.part_number, Part.name)
        .filter(Part.part_number.in_(part_numbers))
        .all()
    )
    return {pn: name for pn, name in rows}
