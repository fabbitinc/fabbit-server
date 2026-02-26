"""Part 목록 Excel 내보내기."""

import uuid
from io import BytesIO

from openpyxl import Workbook
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.mapping import repository as mapping_repo
from app.modules.ontology.schemas import MappingResult
from app.modules.part import repository as repo
from app.modules.part.models import ExtendedPropertyDefinition
from app.queries.part._helpers import auto_fit_columns

# 매핑 없을 때 고정 컬럼 순서
_EXPORT_COLUMN_ORDER = [
    "part_number",
    "name",
    "revision",
    "material",
    "unit",
    "description",
    "category",
    "is_phantom",
    "lifecycle_state",
    "lead_time_days",
]


@transactional(read_only=True)
def export_parts_excel(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    category: str | None = None,
    lifecycle_state: str | None = None,
    has_drawing: bool | None = None,
    has_children: bool | None = None,
    part_ids: list[uuid.UUID] | None = None,
    mapping_id: uuid.UUID | None = None,
) -> bytes:
    """Part 목록을 Excel(xlsx)로 내보내기.

    매핑이 존재하면 원본 엑셀 헤더명을 사용하고, 없으면 온톨로지 속성명을 그대로 사용합니다.
    """
    parts = repo.list_parts_for_export(
        db,
        search=search,
        category=category,
        lifecycle_state=lifecycle_state,
        has_drawing=has_drawing,
        has_children=has_children,
        part_ids=part_ids,
    )

    # 확장 속성 키 합집합 수집
    ext_keys: set[str] = set()
    for part in parts:
        if part.extended_properties:
            ext_keys.update(part.extended_properties.keys())
    sorted_ext_keys = sorted(ext_keys)

    # 매핑 역방향 딕셔너리 구성
    reverse_map: dict[str, str] = {}  # {target_property: source_column}
    mapping_order: list[str] | None = None  # 매핑 순서 기반 컬럼 키 리스트

    if mapping_id:
        result = mapping_repo.get_mapping_by_id(db, mapping_id)
        if result:
            _, revision = result
            mapping_result = MappingResult.model_validate(revision.mapping)
            # property_mappings 순서대로 컬럼 키 + 역방향 매핑
            mapping_order = []
            for pm in mapping_result.property_mappings:
                reverse_map[pm.target_property] = pm.source_column
                mapping_order.append(pm.target_property)

    # 확장 속성 display_name 조회 (매핑에 없는 확장 속성용)
    ext_display_names: dict[str, str] = {}
    if sorted_ext_keys:
        defs = (
            db.query(ExtendedPropertyDefinition)
            .filter(
                ExtendedPropertyDefinition.key.in_(sorted_ext_keys),
                ExtendedPropertyDefinition.target_entity == "Part",
            )
            .all()
        )
        ext_display_names = {d.key: d.display_name for d in defs}

    # 컬럼 키 순서 결정
    if mapping_order is not None:
        # 매핑에 포함된 확장 속성은 이미 mapping_order에 있으므로 나머지만 추가
        mapped_keys = set(mapping_order)
        extra_ext = [k for k in sorted_ext_keys if k not in mapped_keys]
        columns = mapping_order + extra_ext
    else:
        columns = _EXPORT_COLUMN_ORDER + sorted_ext_keys

    # 헤더명 결정
    def _header_name(key: str) -> str:
        if key in reverse_map:
            return reverse_map[key]
        if key in ext_display_names:
            return ext_display_names[key]
        return key

    headers = [_header_name(col) for col in columns]

    # Excel 생성
    wb = Workbook()
    ws = wb.active
    ws.title = "부품목록"
    ws.append(headers)

    standard_attrs = repo._PART_STANDARD_ATTRS | {"part_number", "revision"}
    for part in parts:
        row = []
        for col in columns:
            if col in standard_attrs:
                row.append(getattr(part, col, None))
            else:
                row.append((part.extended_properties or {}).get(col))
        ws.append(row)

    auto_fit_columns(ws)
    buf = BytesIO()
    wb.save(buf)
    return buf.getvalue()
