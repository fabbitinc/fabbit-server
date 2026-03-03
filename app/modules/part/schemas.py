"""부품(Part) 조회 API 스키마."""

from __future__ import annotations

import uuid
from typing import Any

from pydantic import BaseModel, Field

from app.modules.drawing.constants import ConversionStatus
from app.modules.file.schemas import FileItem
from app.modules.user.schemas import UserSummary

# ── 관계 서브 모델 ──


class BomChild(BaseModel):
    """CONSISTS_OF 자식 (depth 1)"""

    id: uuid.UUID
    part_number: str
    name: str | None = None
    quantity: int = 1
    extended_properties: dict[str, Any] = {}


class BomParent(BaseModel):
    """CONSISTS_OF 부모 (depth 1)"""

    id: uuid.UUID
    part_number: str
    name: str | None = None
    quantity: int = 1
    extended_properties: dict[str, Any] = {}


class RelatedDrawing(BaseModel):
    """DEFINED_BY 도면"""

    id: uuid.UUID
    drawing_number: str | None = None
    name: str | None = None
    version: str | None = None
    status: str | None = None
    conversion_status: ConversionStatus | None = None
    thumbnail_url: str | None = None
    pdf_url: str | None = None
    original_file_url: str | None = None


class RelatedSupplier(BaseModel):
    """SUPPLIED_BY 공급사"""

    id: uuid.UUID
    company_name: str
    code: str | None = None
    country: str | None = None
    unit_cost: float | None = None


# ── 첨부파일 ──


class AttachFilesRequest(BaseModel):
    """Part 첨부파일 배치 연결 요청"""

    file_ids: list[uuid.UUID] = Field(..., min_length=1, max_length=20)


# ── Lookup ──


class PartLookupItem(BaseModel):
    """부품 lookup 항목 (picker/autocomplete용)."""

    id: uuid.UUID
    part_number: str
    name: str | None = None


class PartLookupResponse(BaseModel):
    """부품 lookup 응답."""

    items: list[PartLookupItem]


# ── 목록 ──


class PartSummary(BaseModel):
    """Part 목록 아이템"""

    id: uuid.UUID
    part_number: str
    name: str | None = None
    category: str | None = None
    revision: str = "1"
    lifecycle_state: str | None = None
    drawing_number: str | None = None
    children_count: int = 0


class PartListResponse(BaseModel):
    """Part 목록 응답"""

    total: int
    offset: int
    limit: int
    items: list[PartSummary]


class PartFilterOptions(BaseModel):
    """Part 필터 옵션 응답"""

    categories: list[str]
    lifecycle_states: list[str]


# ── 상세 ──


class PartDetailResponse(BaseModel):
    """Part 상세 응답"""

    id: uuid.UUID
    part_number: str
    name: str | None = None
    revision: str = "1"
    material: str | None = None
    unit: str | None = None
    description: str | None = None
    category: str | None = None
    lifecycle_state: str | None = None
    is_phantom: bool | None = None
    lead_time_days: int | None = None
    extended_properties: dict[str, Any] = {}

    # 담당자/팀
    owner_id: uuid.UUID | None = None
    owner: UserSummary | None = None
    owner_team_id: uuid.UUID | None = None
    owner_team_name: str | None = None

    drawing: RelatedDrawing | None = None
    children_count: int = 0
    parents_count: int = 0
    suppliers_count: int = 0
    files_count: int = 0
    projects_count: int = 0


# ── 관계 별도 조회 응답 ──


class PartBomResponse(BaseModel):
    """Part BOM 직접 관계 응답 (1-depth)"""

    children: list[BomChild] = []
    parents: list[BomParent] = []


class PartSuppliersResponse(BaseModel):
    """Part 공급사 목록 응답"""

    total: int
    items: list[RelatedSupplier]


class PartFilesResponse(BaseModel):
    """Part 첨부파일 목록 응답"""

    total: int
    items: list[FileItem]


# ── BOM 트리 ──


class BomTreeNode(BaseModel):
    """BOM 트리 노드 (재귀)"""

    id: uuid.UUID
    part_number: str
    name: str | None = None
    revision: str = "1"
    material: str | None = None
    unit: str | None = None
    category: str | None = None
    lifecycle_state: str | None = None
    quantity: int = 1
    children: list[BomTreeNode] = []


class BomTreeResponse(BaseModel):
    """BOM 트리 응답"""

    root: BomTreeNode
    direction: str
    total_count: int


# ── Part 담당자/팀 ──


class PartOwnerResponse(BaseModel):
    """Part 담당자/팀 응답"""

    owner_id: uuid.UUID | None = None
    owner: UserSummary | None = None
    owner_team_id: uuid.UUID | None = None
    owner_team_name: str | None = None


class UpdatePartOwnerRequest(BaseModel):
    """Part 담당자/팀 수정 요청 (PATCH 시맨틱)

    필드가 body에 포함되면 해당 값으로 설정 (null이면 해제),
    미포함이면 변경하지 않습니다.
    """

    owner_id: uuid.UUID | None = None
    owner_team_id: uuid.UUID | None = None


# ── 카테고리 ──


class CategoryStatsItem(BaseModel):
    """카테고리별 부품 개수"""

    category: str
    part_count: int


class CategoryStatsResponse(BaseModel):
    """카테고리별 부품 개수 응답"""

    items: list[CategoryStatsItem]


class CategoryLookupResponse(BaseModel):
    """카테고리 선택용 경량 목록 응답"""

    items: list[str]


class RenameCategoryRequest(BaseModel):
    """카테고리 이름 변경 요청"""

    new_name: str = Field(..., min_length=1, max_length=200)


# ── 카테고리별 기본 담당자/팀 ──


class PartDefaultOwnerRequest(BaseModel):
    """카테고리 기본 담당자/팀 설정 요청"""

    category: str | None = None
    default_owner_id: uuid.UUID | None = None
    default_owner_team_id: uuid.UUID | None = None


class PartDefaultOwnerItem(BaseModel):
    """카테고리 기본 담당자/팀 설정 항목"""

    id: uuid.UUID
    category: str | None = None
    default_owner_id: uuid.UUID | None = None
    default_owner: UserSummary | None = None
    default_owner_team_id: uuid.UUID | None = None
    default_owner_team_name: str | None = None


class PartDefaultOwnerListResponse(BaseModel):
    """카테고리 기본 담당자/팀 설정 목록"""

    items: list[PartDefaultOwnerItem]
