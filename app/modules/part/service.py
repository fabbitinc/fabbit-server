"""부품(Part) 조회 비즈니스 로직.

속성, BOM 관계, Drawing/Supplier 관계 모두 RDS에서 읽습니다.
"""

import uuid
from io import BytesIO

from openpyxl import Workbook
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.s3_client import s3_client
from app.modules.mapping import repository as mapping_repo
from app.modules.ontology.schemas import MappingResult
from app.modules.file import repository as file_repo
from app.modules.file.constants import FileStatus
from app.modules.part import repository as repo
from app.modules.part.constants import BomDirection
from app.modules.part.models import ExtendedPropertyDefinition
from app.modules.part.schemas import (
    BomChild,
    BomParent,
    BomTreeNode,
    BomTreeResponse,
    PartDetailResponse,
    PartFileItem,
    PartFilterOptions,
    PartListResponse,
    PartSummary,
    RelatedDrawing,
    RelatedSupplier,
)


_s3 = s3_client


# ── Part 목록 ──


@transactional(read_only=True)
def list_parts(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    category: str | None = None,
    lifecycle_state: str | None = None,
    has_drawing: bool | None = None,
    has_children: bool | None = None,
    offset: int = 0,
    limit: int = 20,
) -> PartListResponse:
    rows, total = repo.list_parts_paginated(
        db,
        search=search,
        category=category,
        lifecycle_state=lifecycle_state,
        has_drawing=has_drawing,
        has_children=has_children,
        offset=offset,
        limit=limit,
    )

    items = [PartSummary(**r) for r in rows]

    return PartListResponse(total=total, offset=offset, limit=limit, items=items)


@transactional(read_only=True)
def get_filter_options(db: Session, auth: AuthContext) -> PartFilterOptions:
    """Part 필터 옵션 조회 — 카테고리, 수명주기 상태의 DISTINCT 값."""
    return PartFilterOptions(
        categories=repo.get_distinct_categories(db),
        lifecycle_states=repo.get_distinct_lifecycle_states(db),
    )


# ── Part 상세 ──


@transactional(read_only=True)
def get_part(db: Session, auth: AuthContext, part_id: uuid.UUID) -> PartDetailResponse:
    # 속성: RDS
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    # BOM 관계: RDS JOIN (name 포함)
    children_rows = repo.get_children(db, part.id)
    parents_rows = repo.get_parents(db, part.id)

    # Drawing/Supplier 관계: RDS
    drawing_row = repo.get_drawing(db, part.id)
    suppliers_rows = repo.get_suppliers(db, part.id)

    children = [
        BomChild(
            id=r["id"],
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            extended_properties=r.get("extended_properties", {}),
        )
        for r in children_rows
    ]

    parents = [
        BomParent(
            id=r["id"],
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            extended_properties=r.get("extended_properties", {}),
        )
        for r in parents_rows
    ]

    drawing = (
        RelatedDrawing(
            id=drawing_row["id"],
            drawing_number=drawing_row["drawing_number"],
            name=drawing_row["name"],
            version=drawing_row["version"],
            status=drawing_row["status"],
            conversion_status=drawing_row["conversion_status"],
            thumbnail_url=_s3.get_file_url(drawing_row["thumbnail_key"]),
            pdf_url=_s3.get_file_url(drawing_row["pdf_key"]),
            original_file_url=_s3.get_file_url(drawing_row["original_file_key"]),
        )
        if drawing_row
        else None
    )

    suppliers = [
        RelatedSupplier(
            id=r["id"],
            company_name=r["company_name"],
            code=r["code"],
            country=r["country"],
            unit_cost=r["unit_cost"],
        )
        for r in suppliers_rows
    ]

    extended = {k: v for k, v in (part.extended_properties or {}).items() if v is not None}

    # 첨부파일: UPLOADED 상태만
    all_files = file_repo.get_files_by_owner(db, "part", part.id)
    files = [
        PartFileItem(
            file_id=f.id,
            original_name=f.original_name,
            content_type=f.content_type,
            file_size=f.file_size,
            file_url=_s3.get_file_url(f.file_key),
            created_at=f.created_at,
        )
        for f in all_files
        if f.status == FileStatus.UPLOADED
    ]

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
        drawing=drawing,
        suppliers=suppliers,
        files=files,
    )


# ── 첨부파일 ──


@transactional()
def attach_files_to_part(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    file_ids: list[uuid.UUID],
) -> list[PartFileItem]:
    """완료된 파일들을 Part에 배치 연결."""
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    files = file_repo.get_files_by_ids(db, file_ids)

    # 요청한 ID와 실제 조회 결과 비교
    found_ids = {f.id for f in files}
    missing = set(file_ids) - found_ids
    if missing:
        raise AppError(
            message=f"파일을 찾을 수 없습니다: {missing}",
            code="NOT_FOUND",
        )

    # UPLOADED 상태 검증
    not_uploaded = [f.id for f in files if f.status != FileStatus.UPLOADED]
    if not_uploaded:
        raise AppError(
            message=f"업로드 완료되지 않은 파일이 있습니다: {not_uploaded}",
            code="INVALID_STATE",
        )

    # 이미 다른 소유자에 연결된 파일 검증
    already_owned = [f.id for f in files if f.owner_id is not None]
    if already_owned:
        raise AppError(
            message=f"이미 다른 리소스에 연결된 파일이 있습니다: {already_owned}",
            code="CONFLICT",
        )

    result = []
    for f in files:
        f.owner_type = "part"
        f.owner_id = part.id
        result.append(
            PartFileItem(
                file_id=f.id,
                original_name=f.original_name,
                content_type=f.content_type,
                file_size=f.file_size,
                file_url=_s3.get_file_url(f.file_key),
                created_at=f.created_at,
            )
        )

    return result


@transactional()
def detach_file_from_part(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    file_id: uuid.UUID,
) -> None:
    """Part 첨부파일 1건 소프트 삭제."""
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    file = file_repo.get_file_by_id(db, file_id)
    if not file or file.owner_type != "part" or file.owner_id != part.id:
        raise AppError(
            message=f"Part '{part_id}'에 연결된 파일 '{file_id}'을(를) 찾을 수 없습니다",
            code="NOT_FOUND",
        )

    file.mark_deleted()


# ── BOM 트리 ──


def _make_node(part_info: dict, *, quantity: int = 1) -> BomTreeNode:
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


def _build_bom_tree(
    root_pn: str,
    edges: list[dict],
    parts_map: dict[str, dict],
) -> BomTreeNode:
    """간선 리스트를 트리 구조로 조립.

    각 edge는: {"parent_pn": str, "child_pn": str, "quantity": int}
    """
    root_info = parts_map.get(root_pn, {"id": None, "part_number": root_pn})
    root = _make_node(root_info)
    node_cache: dict[str, BomTreeNode] = {root_pn: root}

    for edge in edges:
        parent_pn = edge["parent_pn"]
        child_pn = edge["child_pn"]
        qty = edge["quantity"]

        if parent_pn not in node_cache:
            p_info = parts_map.get(parent_pn, {"id": None, "part_number": parent_pn})
            node_cache[parent_pn] = _make_node(p_info)

        parent_node = node_cache[parent_pn]

        # 동일 부모-자식 간선 중복 방지
        child_key = f"{parent_pn}->{child_pn}"
        if child_key not in node_cache:
            c_info = parts_map.get(child_pn, {"id": None, "part_number": child_pn})
            child_node = _make_node(c_info, quantity=qty)
            node_cache[child_key] = child_node
            if child_pn not in node_cache:
                node_cache[child_pn] = child_node
            parent_node.children.append(child_node)

    return root


@transactional(read_only=True)
def get_bom_tree(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    direction: BomDirection = BomDirection.FORWARD,
) -> BomTreeResponse:
    """BOM 트리 조회 (정전개/역전개)."""
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    reverse = direction == BomDirection.REVERSE
    edges = repo.get_bom_edges(db, part.id, reverse=reverse)

    # 모든 part_number 수집하여 상세 필드 일괄 조회
    all_pns: set[str] = {part.part_number}
    for edge in edges:
        all_pns.add(edge["parent_pn"])
        all_pns.add(edge["child_pn"])
    parts_map = repo.bulk_get_parts(db, list(all_pns))

    root = _build_bom_tree(
        root_pn=part.part_number,
        edges=edges,
        parts_map=parts_map,
    )

    return BomTreeResponse(
        root=root,
        direction=direction.value,
        total_count=len(all_pns),
    )


# ── Excel 내보내기 ──

# 한글/영문 혼용 시 글자당 대략적인 폭 비율
_CHAR_WIDTH = 1.2
_MIN_COL_WIDTH = 8
_MAX_COL_WIDTH = 50


def _auto_fit_columns(ws) -> None:
    """워크시트 각 컬럼의 너비를 데이터 최대 길이에 맞춤."""
    for col_cells in ws.columns:
        max_len = 0
        for cell in col_cells:
            if cell.value is not None:
                max_len = max(max_len, len(str(cell.value)))
        width = min(max(int(max_len * _CHAR_WIDTH) + 2, _MIN_COL_WIDTH), _MAX_COL_WIDTH)
        ws.column_dimensions[col_cells[0].column_letter].width = width


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

    _auto_fit_columns(ws)
    buf = BytesIO()
    wb.save(buf)
    return buf.getvalue()


# ── BOM Excel 내보내기 ──

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


def _flatten_bom_tree(node: BomTreeNode, level: int = 0) -> list[dict]:
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
        rows.extend(_flatten_bom_tree(child, level + 1))
    return rows


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

    root = _build_bom_tree(root_pn=part.part_number, edges=edges, parts_map=parts_map)
    flat_rows = _flatten_bom_tree(root)

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

    _auto_fit_columns(ws)
    buf = BytesIO()
    wb.save(buf)
    return buf.getvalue()


