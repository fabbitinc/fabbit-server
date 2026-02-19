"""부품(Part) 조회 비즈니스 로직.

속성, BOM 관계, Drawing/Supplier 관계 모두 RDS에서 읽습니다.
"""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.part import repository as repo
from app.modules.part.schemas import (
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


def _safe_int(val, default: int = 0) -> int:
    """agtype에서 파싱된 값을 int로 변환"""
    if val is None:
        return default
    try:
        return int(val)
    except (ValueError, TypeError):
        return default


def _safe_str(val) -> str | None:
    if val is None:
        return None
    s = str(val).strip()
    return s if s else None


# ── Part 목록 ──


@transactional(read_only=True)
def list_parts(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> PartListResponse:
    parts, total = repo.list_parts_paginated(
        db, search=search, offset=offset, limit=limit
    )

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


@transactional(read_only=True)
def get_part(db: Session, auth: AuthContext, part_number: str) -> PartDetailResponse:
    # 속성: RDS
    part = repo.get_by_part_number(db, part_number)
    if not part:
        raise AppError(message=f"Part '{part_number}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    # BOM 관계: RDS JOIN (name 포함)
    children_rows = repo.get_children(db, part.id)
    parents_rows = repo.get_parents(db, part.id)

    # Drawing/Supplier 관계: RDS
    drawings_rows = repo.get_drawings(db, part.id)
    suppliers_rows = repo.get_suppliers(db, part.id)

    children = [
        BomChild(
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            extended_properties=r.get("extended_properties", {}),
        )
        for r in children_rows
    ]

    parents = [
        BomParent(
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            extended_properties=r.get("extended_properties", {}),
        )
        for r in parents_rows
    ]

    drawings = [
        RelatedDrawing(
            drawing_number=r["drawing_number"],
            name=r["name"],
            version=r["version"],
            status=r["status"],
        )
        for r in drawings_rows
    ]

    suppliers = [
        RelatedSupplier(
            company_name=r["company_name"],
            code=r["code"],
            country=r["country"],
            unit_cost=r["unit_cost"],
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
                # 부모로 재조회될 수 있도록 단순 키로도 등록
                if child_pn not in node_cache:
                    node_cache[child_pn] = child_node
                parent_node.children.append(child_node)

    return root


@transactional(read_only=True)
def get_part_bom_tree(
    db: Session,
    auth: AuthContext,
    part_number: str,
) -> BomTreeResponse:
    graph_name = org_id_to_schema(auth.org_id)

    # 루트 Part 존재 확인 (RDS)
    part = repo.get_by_part_number(db, part_number)
    if not part:
        raise AppError(message=f"Part '{part_number}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    paths = repo.get_bom_paths(db, part_number, graph_name)

    # paths에서 모든 part_number 추출하여 name 일괄 조회
    all_pns: set[str] = {part_number}
    for row in paths:
        for pn in (row["c0"] or []):
            if pn:
                all_pns.add(pn)
    name_map = repo.bulk_get_names(db, list(all_pns))

    root = _build_bom_tree(
        root_pn=part_number,
        root_name=part.name,
        paths=paths,
        name_map=name_map,
    )

    return BomTreeResponse(root=root)


