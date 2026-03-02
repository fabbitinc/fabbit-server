"""Activity 도메인 API Pydantic 스키마."""

import uuid
from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel

from app.modules.user.schemas import UserSummary

# ── 통일 Detail 구조 ──


class Ref(BaseModel):
    """Activity detail에서 참조되는 엔티티."""

    id: str
    type: str
    label: str
    meta: dict[str, Any] | None = None


class ChangesDetail(BaseModel):
    """필드 변경 이력. changes: {field: {old: str, new: str}}"""

    changes: dict[str, dict[str, str]]


class DiffDetail(BaseModel):
    """추가/제거 목록."""

    added: list[Ref] = []
    removed: list[Ref] = []


class RefsDetail(BaseModel):
    """참조 목록."""

    refs: list[Ref]


ActivityDetail = ChangesDetail | DiffDetail | RefsDetail | dict[str, Any]


# ── Activity 응답 ──


class ActivityResponse(BaseModel):
    id: uuid.UUID
    action: str
    scope: str | None = None
    actor_id: uuid.UUID
    detail: ActivityDetail | None = None
    created_at: datetime


class ActivityListResponse(BaseModel):
    items: list[ActivityResponse]
    next_cursor: uuid.UUID | None = None
    users: dict[str, UserSummary] = {}


# ── 타임라인 ──


class TimelineCommentItem(BaseModel):
    type: Literal["comment"] = "comment"
    id: uuid.UUID
    body: dict | None = None
    author_id: uuid.UUID | None = None
    created_at: datetime
    updated_at: datetime
    is_modified: bool


class TimelineActivityItem(BaseModel):
    type: Literal["activity"] = "activity"
    id: uuid.UUID
    action: str
    scope: str | None = None
    actor_id: uuid.UUID
    detail: ActivityDetail | None = None
    created_at: datetime


class TimelineResponse(BaseModel):
    items: list[TimelineCommentItem | TimelineActivityItem]
    users: dict[str, UserSummary] = {}
