"""라벨 도메인 API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


# ── 요청 ──


class CreateLabelRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=50, description="라벨 이름")
    description: str | None = Field(None, max_length=200, description="라벨 설명")
    color: str = Field(
        ..., min_length=7, max_length=7, pattern=r"^#[0-9a-fA-F]{6}$", description="라벨 색상 (hex #RRGGBB)"
    )


class UpdateLabelRequest(BaseModel):
    name: str | None = Field(None, min_length=1, max_length=50, description="라벨 이름")
    description: str | None = Field(None, max_length=200, description="라벨 설명")
    color: str | None = Field(
        None, min_length=7, max_length=7, pattern=r"^#[0-9a-fA-F]{6}$", description="라벨 색상 (hex #RRGGBB)"
    )


# ── 응답 ──


class LabelResponse(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None
    color: str
    created_at: datetime
    created_by: uuid.UUID | None = None


class LabelListResponse(BaseModel):
    total: int
    items: list[LabelResponse]


# ── Lookup ──


class LabelLookupItem(BaseModel):
    """라벨 lookup 항목 (picker/autocomplete용)."""

    id: uuid.UUID
    name: str
    color: str


class LabelLookupResponse(BaseModel):
    """라벨 lookup 응답."""

    items: list[LabelLookupItem]
