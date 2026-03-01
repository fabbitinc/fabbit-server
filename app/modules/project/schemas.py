"""프로젝트(Project) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.modules.user.schemas import UserSummary


# ── 요청 ──


class CreateProjectRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=200, description="프로젝트 이름")
    description: str | None = Field(None, description="프로젝트 설명")


class UpdateProjectRequest(BaseModel):
    name: str | None = Field(None, min_length=1, max_length=200, description="프로젝트 이름")
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
    part_count: int
    open_issue_count: int
    open_change_request_count: int
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


# ── 멤버 lookup ──


class MemberLookupResponse(BaseModel):
    """멤버 lookup 응답 (picker/autocomplete용)."""

    items: list[UserSummary]


# ── 멤버 ──


class MemberSummary(BaseModel):
    """조직 멤버 요약."""
    user_id: uuid.UUID
    full_name: str
    email: str
    role: str
    job_role: str | None = None
    profile_image_url: str | None = None


class MemberListResponse(BaseModel):
    """조직 멤버 목록."""
    items: list[MemberSummary]


class ProjectMemberSummary(BaseModel):
    """프로젝트 멤버 요약."""
    user_id: uuid.UUID
    full_name: str
    email: str


class ProjectMemberListResponse(BaseModel):
    """프로젝트 멤버 목록."""
    items: list[ProjectMemberSummary]


class ManageMembersRequest(BaseModel):
    """멤버 추가/제거 요청."""
    user_ids: list[uuid.UUID]


class ManageMembersResponse(BaseModel):
    """멤버 추가/제거 결과."""
    count: int
