"""프로젝트 트리 및 CRUD API 스키마."""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


# ── 트리 조회 스키마 (기존) ──


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


# ── Project CRUD 스키마 ──


class CreateProjectRequest(BaseModel):
    name: str = Field(..., max_length=200)
    description: str | None = None


class UpdateProjectRequest(BaseModel):
    name: str | None = Field(None, max_length=200)
    description: str | None = None


class ProjectResponse(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None
    created_at: datetime
    updated_at: datetime


# ── Folder CRUD 스키마 ──


class CreateFolderRequest(BaseModel):
    name: str = Field(..., max_length=200)
    project_id: uuid.UUID
    parent_id: uuid.UUID | None = None


class UpdateFolderRequest(BaseModel):
    name: str | None = Field(None, max_length=200)


class MoveFolderRequest(BaseModel):
    parent_id: uuid.UUID | None = None  # null = 루트로 이동


class FolderResponse(BaseModel):
    id: uuid.UUID
    name: str
    parent_id: uuid.UUID | None
    project_id: uuid.UUID | None
    created_at: datetime


# ── ProjectPart 스키마 ──


class ProjectPartResponse(BaseModel):
    id: uuid.UUID
    part_number: str
    name: str | None
    category: str | None


class ProjectPartListResponse(BaseModel):
    parts: list[ProjectPartResponse]
