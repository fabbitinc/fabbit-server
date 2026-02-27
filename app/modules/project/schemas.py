"""프로젝트(Project) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


# ── 요청 ──


class CreateProjectRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=200, description="프로젝트 이름")
    description: str | None = Field(None, description="프로젝트 설명")


# ── 응답 ──


class ProjectSummary(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None
    part_count: int


class ProjectListResponse(BaseModel):
    total: int
    offset: int
    limit: int
    items: list[ProjectSummary]


class ProjectDetailResponse(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None
    created_at: datetime
    updated_at: datetime


# ── Project ↔ Part 연결 ──


class LinkPartsRequest(BaseModel):
    part_ids: list[uuid.UUID]


class LinkPartsResponse(BaseModel):
    linked_count: int


class ProjectPartSummary(BaseModel):
    id: uuid.UUID
    part_number: str
    name: str | None = None


class ProjectPartsResponse(BaseModel):
    total: int
    items: list[ProjectPartSummary]


class PartProjectSummary(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None


class PartProjectsResponse(BaseModel):
    total: int
    items: list[PartProjectSummary]
