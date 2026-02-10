"""아이템(Part) 조회 비즈니스 로직."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.item import repository as repo
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
    graph_name = org_id_to_schema(auth.org_id)

    total = repo.count_parts(db, graph_name, search)
    rows = repo.list_parts(db, graph_name, search=search, offset=offset, limit=limit)

    items = []
    for row in rows:
        # 다중 컬럼: c0~c6
        items.append(
            PartSummary(
                part_number=row["c0"] or "",
                name=_safe_str(row["c1"]),
                category=_safe_str(row["c2"]),
                lifecycle_state=_safe_str(row["c3"]),
                child_count=_safe_int(row["c4"]),
                drawing_count=_safe_int(row["c5"]),
                supplier_count=_safe_int(row["c6"]),
            )
        )

    return PartListResponse(total=total, offset=offset, limit=limit, items=items)


# ── Part 상세 ──


def _extract_extended_properties(props: dict) -> dict:
    """_ext_ 프리픽스 속성을 분리"""
    ext = {}
    for key, val in props.items():
        if key.startswith("_ext_") and val is not None:
            ext[key] = val
    return ext


def get_item(db: Session, auth: AuthContext, part_number: str) -> PartDetailResponse:
    graph_name = org_id_to_schema(auth.org_id)

    props = repo.get_part_vertex(db, graph_name, part_number)
    if not props:
        raise AppError(message=f"Part '{part_number}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    # 관계 4종 조회
    children_rows = repo.get_children(db, graph_name, part_number)
    parents_rows = repo.get_parents(db, graph_name, part_number)
    drawings_rows = repo.get_drawings(db, graph_name, part_number)
    suppliers_rows = repo.get_suppliers(db, graph_name, part_number)

    children = [
        BomChild(
            part_number=r["c0"] or "",
            name=_safe_str(r["c1"]),
            quantity=_safe_int(r["c2"], 1),
            sequence=_safe_int(r["c3"]) or None,
            reference_designator=_safe_str(r["c4"]),
            find_number=_safe_str(r["c5"]),
        )
        for r in children_rows
    ]

    parents = [
        BomParent(
            part_number=r["c0"] or "",
            name=_safe_str(r["c1"]),
            quantity=_safe_int(r["c2"], 1),
            sequence=_safe_int(r["c3"]) or None,
            reference_designator=_safe_str(r["c4"]),
            find_number=_safe_str(r["c5"]),
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

    extended = _extract_extended_properties(props)

    return PartDetailResponse(
        part_number=props.get("part_number", ""),
        name=_safe_str(props.get("name")),
        revision=_safe_str(props.get("revision")),
        material=_safe_str(props.get("material")),
        unit=_safe_str(props.get("unit")),
        description=_safe_str(props.get("description")),
        category=_safe_str(props.get("category")),
        lifecycle_state=_safe_str(props.get("lifecycle_state")),
        is_phantom=props.get("is_phantom"),
        lead_time_days=_safe_int(props.get("lead_time_days")) or None,
        extended_properties=extended,
        children=children,
        parents=parents,
        drawings=drawings,
        suppliers=suppliers,
    )


# ── BOM 트리 ──


def _build_bom_tree(root_pn: str, root_name: str | None, paths: list[dict]) -> BomTreeNode:
    """경로 리스트를 트리 구조로 조립.

    각 path row는:
      c0: [root_pn, child1_pn, child2_pn, ...]  (nodes의 part_number)
      c1: [root_name, child1_name, ...]           (nodes의 name)
      c2: [qty1, qty2, ...]                        (relationships의 quantity)
      c3: [ref1, ref2, ...]                        (relationships의 reference_designator)
    """
    # 트리 노드 캐시: part_number → BomTreeNode
    node_cache: dict[str, BomTreeNode] = {}
    root = BomTreeNode(part_number=root_pn, name=root_name)
    node_cache[root_pn] = root

    for row in paths:
        pn_path = row["c0"] or []
        name_path = row["c1"] or []
        qty_path = row["c2"] or []
        ref_path = row["c3"] or []

        # 경로의 각 edge를 순회하며 부모-자식 연결
        for i in range(len(pn_path) - 1):
            parent_pn = pn_path[i]
            child_pn = pn_path[i + 1]
            child_name = name_path[i + 1] if i + 1 < len(name_path) else None
            qty = _safe_int(qty_path[i] if i < len(qty_path) else None, 1)
            ref = _safe_str(ref_path[i] if i < len(ref_path) else None)

            # 부모 노드 확보
            if parent_pn not in node_cache:
                parent_name = name_path[i] if i < len(name_path) else None
                node_cache[parent_pn] = BomTreeNode(
                    part_number=parent_pn, name=_safe_str(parent_name)
                )

            parent_node = node_cache[parent_pn]

            # 자식 노드 — 같은 부모 아래 중복 방지
            child_key = f"{parent_pn}->{child_pn}"
            if child_key not in node_cache:
                child_node = BomTreeNode(
                    part_number=child_pn,
                    name=_safe_str(child_name),
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

    # 루트 Part 존재 확인
    props = repo.get_part_vertex(db, graph_name, part_number)
    if not props:
        raise AppError(message=f"Part '{part_number}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    paths = repo.get_bom_paths(db, graph_name, part_number)

    root = _build_bom_tree(
        root_pn=part_number,
        root_name=_safe_str(props.get("name")),
        paths=paths,
    )

    return BomTreeResponse(root=root)
