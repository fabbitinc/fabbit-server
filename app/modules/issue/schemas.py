"""이슈 도메인 API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


# ── 요청 ──


class CreateIssueRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=500, description="이슈 제목")
    body: str | None = Field(None, description="이슈 본문")


class CreateChangeRequestRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=500, description="변경 요청 제목")
    body: str | None = Field(None, description="변경 요청 본문")


# ── 응답 ──


class IssueResponse(BaseModel):
    id: uuid.UUID
    project_id: uuid.UUID
    number: int
    type: str
    title: str
    body: str | None = None
    state: str
    closed_at: datetime | None = None
    created_at: datetime
    created_by: uuid.UUID | None = None


class ChangeRequestResponse(IssueResponse):
    cr_state: str
    merged_at: datetime | None = None
    merged_by: uuid.UUID | None = None


# ── 담당자 ──


class AssignUsersRequest(BaseModel):
    user_ids: list[uuid.UUID] = Field(..., min_length=1, description="할당할 사용자 ID 목록")


class AssignUsersResponse(BaseModel):
    assigned_count: int


# ── 부품 연결 ──


class LinkPartsRequest(BaseModel):
    part_ids: list[uuid.UUID] = Field(..., min_length=1, description="연결할 부품 ID 목록")


class LinkPartsResponse(BaseModel):
    linked_count: int


# ── 첨부파일 ──


class AttachFilesRequest(BaseModel):
    file_ids: list[uuid.UUID] = Field(
        ..., min_length=1, max_length=20, description="첨부할 파일 ID 목록 (최대 20개)"
    )
