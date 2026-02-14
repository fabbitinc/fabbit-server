"""프로젝트 트리 API 스키마."""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


class ProjectStats(BaseModel):
    upload_count: int = 0
    drawing_count: int = 0
    folder_count: int = 0


class FolderStats(BaseModel):
    drawing_count: int = 0


class FolderTreeNode(BaseModel):
    id: uuid.UUID
    name: str
    parent_id: uuid.UUID | None = None
    project_id: uuid.UUID | None = None
    created_at: datetime
    stats: FolderStats = Field(default_factory=FolderStats)
    folders: list["FolderTreeNode"] = Field(default_factory=list)
    items: list[dict[str, Any]] = Field(default_factory=list)
    item_count: int = 0


class ProjectTreeNode(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None
    created_at: datetime
    updated_at: datetime
    stats: ProjectStats = Field(default_factory=ProjectStats)
    folders: list[FolderTreeNode] = Field(default_factory=list)


class ProjectTreeMeta(BaseModel):
    project_count: int
    folder_count: int


class ProjectTreeResponse(BaseModel):
    projects: list[ProjectTreeNode] = Field(default_factory=list)
    orphans: list[FolderTreeNode] = Field(default_factory=list)
    meta: ProjectTreeMeta
