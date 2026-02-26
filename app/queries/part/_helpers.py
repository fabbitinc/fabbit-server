"""Part 쿼리 공유 헬퍼 — BOM 트리 조립, Excel 유틸."""

from app.modules.part.schemas import BomTreeNode

# ── BOM 트리 조립 ──


def make_node(part_info: dict, *, quantity: int = 1) -> BomTreeNode:
    """Part 정보 dict로 BomTreeNode 생성."""
    return BomTreeNode(
        id=part_info["id"],
        part_number=part_info["part_number"],
        name=part_info.get("name"),
        revision=part_info.get("revision", "1"),
        material=part_info.get("material"),
        unit=part_info.get("unit"),
        category=part_info.get("category"),
        lifecycle_state=part_info.get("lifecycle_state"),
        quantity=quantity,
    )


def build_bom_tree(
    root_pn: str,
    edges: list[dict],
    parts_map: dict[str, dict],
) -> BomTreeNode:
    """간선 리스트를 트리 구조로 조립.

    각 edge는: {"parent_pn": str, "child_pn": str, "quantity": int}
    """
    root_info = parts_map.get(root_pn, {"id": None, "part_number": root_pn})
    root = make_node(root_info)
    node_cache: dict[str, BomTreeNode] = {root_pn: root}

    for edge in edges:
        parent_pn = edge["parent_pn"]
        child_pn = edge["child_pn"]
        qty = edge["quantity"]

        if parent_pn not in node_cache:
            p_info = parts_map.get(parent_pn, {"id": None, "part_number": parent_pn})
            node_cache[parent_pn] = make_node(p_info)

        parent_node = node_cache[parent_pn]

        # 동일 부모-자식 간선 중복 방지
        child_key = f"{parent_pn}->{child_pn}"
        if child_key not in node_cache:
            c_info = parts_map.get(child_pn, {"id": None, "part_number": child_pn})
            child_node = make_node(c_info, quantity=qty)
            node_cache[child_key] = child_node
            if child_pn not in node_cache:
                node_cache[child_pn] = child_node
            parent_node.children.append(child_node)

    return root


# ── BOM 트리 펼침 ──


def flatten_bom_tree(node: BomTreeNode, level: int = 0) -> list[dict]:
    """BomTreeNode를 flat rows로 재귀 펼침."""
    row = {
        "level": level,
        "part_number": node.part_number,
        "name": node.name,
        "revision": node.revision,
        "quantity": node.quantity,
        "material": node.material,
        "unit": node.unit,
        "category": node.category,
        "lifecycle_state": node.lifecycle_state,
    }
    rows = [row]
    for child in node.children:
        rows.extend(flatten_bom_tree(child, level + 1))
    return rows


# ── Excel 유틸 ──

# 한글/영문 혼용 시 글자당 대략적인 폭 비율
CHAR_WIDTH = 1.2
MIN_COL_WIDTH = 8
MAX_COL_WIDTH = 50


def auto_fit_columns(ws) -> None:
    """워크시트 각 컬럼의 너비를 데이터 최대 길이에 맞춤."""
    for col_cells in ws.columns:
        max_len = 0
        for cell in col_cells:
            if cell.value is not None:
                max_len = max(max_len, len(str(cell.value)))
        width = min(max(int(max_len * CHAR_WIDTH) + 2, MIN_COL_WIDTH), MAX_COL_WIDTH)
        ws.column_dimensions[col_cells[0].column_letter].width = width
