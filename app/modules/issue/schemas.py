"""이슈 도메인 API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.modules.file.schemas import FileItem


# ── 요청 ──


class CreateIssueRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=500, description="이슈 제목")
    body: str | None = Field(None, description="이슈 본문")


class CreateChangeRequestRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=500, description="변경 요청 제목")
    body: str | None = Field(None, description="변경 요청 본문")


# ── 목록 응답 (body 제외한 요약) ──


class LabelBadge(BaseModel):
    """라벨 배지 (목록 표시용)."""

    id: uuid.UUID
    name: str
    color: str


class AssigneeSummary(BaseModel):
    """담당자 요약 (아바타 표시용)."""

    id: uuid.UUID
    full_name: str


class PartBadge(BaseModel):
    """연결 부품 배지 (목록 표시용)."""

    id: uuid.UUID
    part_number: str
    name: str | None = None


class IssueSummary(BaseModel):
    id: uuid.UUID
    project_id: uuid.UUID
    number: int
    type: str
    title: str
    state: str
    closed_at: datetime | None = None
    created_at: datetime
    updated_at: datetime
    created_by: uuid.UUID | None = None
    created_by_name: str | None = None
    labels: list[LabelBadge] = []
    assignees: list[AssigneeSummary] = []
    parts: list[PartBadge] = []
    files: list[FileItem] = []
    comments_count: int = 0


class IssueListResponse(BaseModel):
    open_count: int
    closed_count: int
    total: int
    offset: int
    limit: int
    items: list[IssueSummary]


class ChangeRequestSummary(IssueSummary):
    cr_state: str
    merged_at: datetime | None = None
    merged_by: uuid.UUID | None = None


class ChangeRequestListResponse(BaseModel):
    open_count: int
    closed_count: int
    total: int
    offset: int
    limit: int
    items: list[ChangeRequestSummary]


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
    updated_at: datetime
    created_by: uuid.UUID | None = None
    created_by_name: str | None = None
    labels: list[LabelBadge] = []
    assignees: list[AssigneeSummary] = []
    parts: list[PartBadge] = []
    files: list[FileItem] = []
    comments_count: int = 0


class ChangeRequestResponse(IssueResponse):
    cr_state: str
    merged_at: datetime | None = None
    merged_by: uuid.UUID | None = None


# ── 담당자 ──


class AssignUsersRequest(BaseModel):
    user_ids: list[uuid.UUID] = Field(..., min_length=1, description="할당할 사용자 ID 목록")


class AssignUsersResponse(BaseModel):
    assigned_count: int


# ── 라벨 연결 ──


class LinkLabelsRequest(BaseModel):
    label_ids: list[uuid.UUID] = Field(..., min_length=1, description="연결할 라벨 ID 목록")


class LinkLabelsResponse(BaseModel):
    linked_count: int


# ── 부품 연결 ──


class LinkPartsRequest(BaseModel):
    part_ids: list[uuid.UUID] = Field(..., min_length=1, description="연결할 부품 ID 목록")


class LinkPartsResponse(BaseModel):
    linked_count: int


# ── 첨부파일 ──


# ── 댓글 ──


class CreateCommentRequest(BaseModel):
    body: str = Field(..., min_length=1, max_length=10000, description="댓글 본문")


class UpdateCommentRequest(BaseModel):
    body: str = Field(..., min_length=1, max_length=10000, description="댓글 본문")


class CommentResponse(BaseModel):
    id: uuid.UUID
    issue_id: uuid.UUID
    body: str
    created_at: datetime
    updated_at: datetime
    created_by: uuid.UUID | None = None


# ── CR-Issue 연결 ──


class LinkIssuesRequest(BaseModel):
    issue_ids: list[uuid.UUID] = Field(..., min_length=1, description="연결할 이슈 ID 목록")


class LinkIssuesResponse(BaseModel):
    linked_count: int


# ── 첨부파일 ──


class AttachFilesRequest(BaseModel):
    file_ids: list[uuid.UUID] = Field(
        ..., min_length=1, max_length=20, description="첨부할 파일 ID 목록 (최대 20개)"
    )
