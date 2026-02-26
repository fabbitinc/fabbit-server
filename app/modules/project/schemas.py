"""프로젝트(Project) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel


class ProjectSummary(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None


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
