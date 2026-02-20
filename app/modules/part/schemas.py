"""부품(Part) 조회 API 스키마."""

from __future__ import annotations

import uuid
from typing import Any

from pydantic import BaseModel


# ── 관계 서브 모델 ──


class BomChild(BaseModel):
    """CONSISTS_OF 자식 (depth 1)"""

    part_number: str
    name: str | None = None
    quantity: int = 1
    extended_properties: dict[str, Any] = {}


class BomParent(BaseModel):
    """CONSISTS_OF 부모 (depth 1)"""

    part_number: str
    name: str | None = None
    quantity: int = 1
    extended_properties: dict[str, Any] = {}


class RelatedDrawing(BaseModel):
    """DEFINED_BY 도면"""

    drawing_number: str
    name: str | None = None
    version: str | None = None
    status: str | None = None


class RelatedSupplier(BaseModel):
    """SUPPLIED_BY 공급사"""

    company_name: str
    code: str | None = None
    country: str | None = None
    unit_cost: float | None = None


# ── 목록 ──


class PartSummary(BaseModel):
    """Part 목록 아이템"""

    id: uuid.UUID
    part_number: str
    name: str | None = None
    category: str | None = None
    revision: str | None = None
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
    revision: str | None = None
    material: str | None = None
    unit: str | None = None
    description: str | None = None
    category: str | None = None
    lifecycle_state: str | None = None
    is_phantom: bool | None = None
    lead_time_days: int | None = None
    extended_properties: dict[str, Any] = {}
    children: list[BomChild] = []
    parents: list[BomParent] = []
    drawings: list[RelatedDrawing] = []
    suppliers: list[RelatedSupplier] = []


# ── BOM 트리 ──


class BomTreeNode(BaseModel):
    """BOM 트리 노드 (재귀)"""

    part_number: str
    name: str | None = None
    quantity: int = 1
    reference_designator: str | None = None
    children: list[BomTreeNode] = []


class BomTreeResponse(BaseModel):
    """BOM 트리 응답"""

    root: BomTreeNode
