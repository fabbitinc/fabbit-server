"""BOM 트리 Excel 내보내기."""

import uuid
from io import BytesIO

from openpyxl import Workbook
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.mapping import repository as mapping_repo
from app.modules.ontology.schemas import MappingResult
from app.modules.part import repository as repo
from app.modules.part.constants import BomDirection
from app.queries.part._helpers import auto_fit_columns, build_bom_tree, flatten_bom_tree

# BOM 트리 펼침 시 고정 컬럼 순서
_BOM_EXPORT_COLUMNS = [
    "level",
    "part_number",
    "name",
    "revision",
    "quantity",
    "material",
    "unit",
    "category",
    "lifecycle_state",
]


@transactional(read_only=True)
def export_bom_excel(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    *,
    direction: BomDirection = BomDirection.FORWARD,
    mapping_id: uuid.UUID | None = None,
) -> bytes:
    """특정 Part의 BOM 트리를 Excel(xlsx)로 내보내기.

    get_bom_tree와 동일한 트리를 flat rows로 펼쳐서 Excel로 반환합니다.
    매핑이 존재하면 원본 엑셀 헤더명을 사용합니다.
    """
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    reverse = direction == BomDirection.REVERSE
    edges = repo.get_bom_edges(db, part.id, reverse=reverse)

    all_pns: set[str] = {part.part_number}
    for edge in edges:
        all_pns.add(edge["parent_pn"])
        all_pns.add(edge["child_pn"])
    parts_map = repo.bulk_get_parts(db, list(all_pns))

    root = build_bom_tree(root_pn=part.part_number, edges=edges, parts_map=parts_map)
    flat_rows = flatten_bom_tree(root)

    # 매핑 역방향 딕셔너리 구성
    reverse_map: dict[str, str] = {}
    if mapping_id:
        result = mapping_repo.get_mapping_by_id(db, mapping_id)
        if result:
            _, revision = result
            mapping_result = MappingResult.model_validate(revision.mapping)
            for pm in mapping_result.property_mappings:
                reverse_map[pm.target_property] = pm.source_column
            for rm in mapping_result.relation_mappings:
                if rm.rel_type == "CONSISTS_OF":
                    for prop, col in rm.rel_columns.items():
                        reverse_map[prop] = col

    columns = _BOM_EXPORT_COLUMNS
    headers = [reverse_map.get(col, col) for col in columns]

    # Excel 생성
    wb = Workbook()
    ws = wb.active
    ws.title = "BOM"
    ws.append(headers)

    for row in flat_rows:
        ws.append([row.get(col) for col in columns])

    auto_fit_columns(ws)
    buf = BytesIO()
    wb.save(buf)
    return buf.getvalue()
