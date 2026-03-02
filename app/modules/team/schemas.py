"""팀(Team) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.modules.user.schemas import UserSummary


# ── 요청 ──


class CreateTeamRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, description="팀 이름")
    description: str | None = Field(None, description="팀 설명")


class UpdateTeamRequest(BaseModel):
    name: str | None = Field(None, min_length=1, max_length=100, description="팀 이름")
    description: str | None = Field(None, description="팀 설명")


# ── 응답 ──


class TeamSummary(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None
    member_count: int
    created_by: uuid.UUID
    created_at: datetime


class TeamListResponse(BaseModel):
    items: list[TeamSummary]


class TeamDetailResponse(BaseModel):
    id: uuid.UUID
    name: str
    description: str | None = None
    member_count: int
    created_by: uuid.UUID
    created_at: datetime
    updated_at: datetime


# ── Lookup ──


class TeamLookupItem(BaseModel):
    """팀 lookup 항목 (picker/autocomplete용)."""

    id: uuid.UUID
    name: str


class TeamLookupResponse(BaseModel):
    """팀 lookup 응답 (picker/autocomplete용)."""

    items: list[TeamLookupItem]


# ── 멤버 ──


class AddTeamMembersRequest(BaseModel):
    """멤버 추가 요청."""

    user_ids: list[uuid.UUID]


class RemoveTeamMembersRequest(BaseModel):
    """멤버 제거 요청."""

    user_ids: list[uuid.UUID]


class ManageTeamMembersResponse(BaseModel):
    """멤버 추가/제거 결과."""

    count: int


class TeamMemberSummary(UserSummary):
    """팀 멤버 요약."""

    pass


class TeamMemberListResponse(BaseModel):
    """팀 멤버 목록."""

    items: list[TeamMemberSummary]
