"""이슈 도메인 API Pydantic 스키마."""

import uuid
from typing import Any
from datetime import datetime

from pydantic import BaseModel, Field, field_validator, model_validator

from app.modules.file.schemas import FileItem
from app.modules.user.schemas import UserSummary


# ── TipTap JSON 검증 ──

# 허용 노드 타입
ALLOWED_NODE_TYPES: frozenset[str] = frozenset({
    "doc", "paragraph", "text", "heading",
    "bulletList", "orderedList", "listItem", "taskList", "taskItem",
    "blockquote", "codeBlock", "hardBreak", "horizontalRule",
    "image", "mention", "userMention", "issueMention",
    "table", "tableRow", "tableCell", "tableHeader",
})

# 허용 마크 타입
ALLOWED_MARK_TYPES: frozenset[str] = frozenset({
    "bold", "italic", "strike", "underline",
    "code", "link", "highlight", "textStyle",
    "superscript", "subscript",
})


class TipTapMark(BaseModel):
    """TipTap 텍스트 마크 (bold, italic, link 등)."""

    type: str
    attrs: dict[str, Any] | None = None

    @field_validator("type")
    @classmethod
    def check_mark_type(cls, v: str) -> str:
        if v not in ALLOWED_MARK_TYPES:
            raise ValueError(f"허용되지 않는 마크 타입: {v}")
        return v

    @model_validator(mode="after")
    def check_link_href(self) -> "TipTapMark":
        """link 마크의 href가 안전한 프로토콜인지 검증."""
        if self.type == "link" and self.attrs:
            href = self.attrs.get("href", "")
            if href and not href.startswith(("http://", "https://", "mailto:")):
                raise ValueError("link href는 http/https/mailto만 허용됩니다")
        return self


class TipTapNode(BaseModel):
    """TipTap JSON 노드 (재귀 구조)."""

    type: str
    text: str | None = None
    content: list["TipTapNode"] | None = None
    attrs: dict[str, Any] | None = None
    marks: list[TipTapMark] | None = None

    @field_validator("type")
    @classmethod
    def check_node_type(cls, v: str) -> str:
        if v not in ALLOWED_NODE_TYPES:
            raise ValueError(f"허용되지 않는 노드 타입: {v}")
        return v

    @model_validator(mode="after")
    def check_image_src(self) -> "TipTapNode":
        """image 노드의 src가 안전한 프로토콜인지 검증."""
        if self.type == "image" and self.attrs:
            src = self.attrs.get("src", "")
            if src and not src.startswith(("http://", "https://")):
                raise ValueError("image src는 http/https만 허용됩니다")
        return self

    @model_validator(mode="after")
    def check_mention_attrs(self) -> "TipTapNode":
        """userMention/issueMention 노드의 attrs(id, label) 검증."""
        if self.type not in ("userMention", "issueMention"):
            return self
        if not self.attrs:
            raise ValueError(f"{self.type}에는 attrs가 필요합니다")
        if "id" not in self.attrs or "label" not in self.attrs:
            raise ValueError(f"{self.type} attrs에는 id와 label이 필요합니다")
        # id가 유효한 UUID인지 검증
        try:
            uuid.UUID(str(self.attrs["id"]))
        except (ValueError, AttributeError):
            raise ValueError(f"{self.type} attrs.id는 유효한 UUID여야 합니다")
        if not isinstance(self.attrs["label"], str):
            raise ValueError(f"{self.type} attrs.label은 문자열이어야 합니다")
        return self


class TipTapDocument(BaseModel):
    """TipTap JSON 문서 루트."""

    type: str = "doc"
    content: list[TipTapNode] | None = None

    @field_validator("type")
    @classmethod
    def check_doc_type(cls, v: str) -> str:
        if v != "doc":
            raise ValueError("최상위 타입은 'doc'이어야 합니다")
        return v


# ── 요청 ──


class CreateIssueRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=500, description="이슈 제목")
    body: TipTapDocument | None = Field(None, description="이슈 본문 (TipTap JSON)")


class UpdateIssueRequest(BaseModel):
    title: str | None = Field(None, min_length=1, max_length=500, description="이슈 제목")
    body: TipTapDocument | None = Field(None, description="이슈 본문 (TipTap JSON)")


class CreateChangeRequestRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=500, description="변경 요청 제목")
    body: TipTapDocument | None = Field(None, description="변경 요청 본문 (TipTap JSON)")
    issue_number: int | None = Field(None, description="연결할 이슈 번호 (ISSUE 타입만 허용)")


# ── 목록 응답 (body 제외한 요약) ──


class LabelBadge(BaseModel):
    """라벨 배지 (목록 표시용)."""

    id: uuid.UUID
    name: str
    color: str


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
    created_by: UserSummary | None = None
    labels: list[LabelBadge] = []
    assignees: list[UserSummary] = []
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
    reviewers: list[UserSummary] = []


class ChangeRequestListResponse(BaseModel):
    open_count: int
    closed_count: int
    total: int
    offset: int
    limit: int
    items: list[ChangeRequestSummary]


# ── 연결 배지 ──


class IssueLookupItem(BaseModel):
    """이슈 lookup 항목 (picker/autocomplete용)."""

    id: uuid.UUID
    number: int
    title: str
    state: str


class IssueLookupResponse(BaseModel):
    """이슈 lookup 응답."""

    items: list[IssueLookupItem]


class LinkedIssueBadge(BaseModel):
    """연결된 이슈 요약 (CR 상세에서 사용)."""

    id: uuid.UUID
    number: int
    title: str
    state: str


class LinkedChangeRequestBadge(BaseModel):
    """연결된 변경 요청 요약 (이슈 상세에서 사용)."""

    id: uuid.UUID
    number: int
    title: str
    state: str
    cr_state: str


# ── 응답 ──


class IssueResponse(BaseModel):
    id: uuid.UUID
    project_id: uuid.UUID
    number: int
    type: str
    title: str
    body: dict | None = None
    state: str
    closed_at: datetime | None = None
    created_at: datetime
    updated_at: datetime
    created_by: UserSummary | None = None
    labels: list[LabelBadge] = []
    assignees: list[UserSummary] = []
    parts: list[PartBadge] = []
    files: list[FileItem] = []
    comments_count: int = 0
    linked_changes: list[LinkedChangeRequestBadge] = []


class ChangeRequestResponse(IssueResponse):
    cr_state: str
    merged_at: datetime | None = None
    merged_by: uuid.UUID | None = None
    reviewers: list[UserSummary] = []
    linked_issues: list[LinkedIssueBadge] = []


# ── 담당자 동기화 ──


class SyncAssigneesRequest(BaseModel):
    user_ids: list[uuid.UUID] = Field(
        default_factory=list, description="동기화할 담당자 ID 목록 (빈 목록 = 모든 담당자 해제)"
    )


class SyncAssigneesResponse(BaseModel):
    added_count: int
    removed_count: int


# ── 검토자 동기화 ──


class SyncReviewersRequest(BaseModel):
    user_ids: list[uuid.UUID] = Field(
        default_factory=list, description="동기화할 검토자 ID 목록 (빈 목록 = 모든 검토자 해제)"
    )


class SyncReviewersResponse(BaseModel):
    added_count: int
    removed_count: int


# ── 라벨 동기화 ──


class SyncLabelsRequest(BaseModel):
    label_ids: list[uuid.UUID] = Field(
        default_factory=list, description="동기화할 라벨 ID 목록 (빈 목록 = 모든 라벨 해제)"
    )


class SyncLabelsResponse(BaseModel):
    added_count: int
    removed_count: int


# ── 부품 동기화 ──


class SyncPartsRequest(BaseModel):
    part_ids: list[uuid.UUID] = Field(
        default_factory=list, description="동기화할 부품 ID 목록 (빈 목록 = 모든 부품 해제)"
    )


class SyncPartsResponse(BaseModel):
    added_count: int
    removed_count: int


# ── 첨부파일 ──


# ── 댓글 ──


class CreateCommentRequest(BaseModel):
    body: TipTapDocument = Field(..., description="댓글 본문 (TipTap JSON)")


class UpdateCommentRequest(BaseModel):
    body: TipTapDocument = Field(..., description="댓글 본문 (TipTap JSON)")


class CommentResponse(BaseModel):
    id: uuid.UUID
    issue_id: uuid.UUID
    body: dict | None = None
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
