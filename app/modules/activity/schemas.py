"""Activity 도메인 API Pydantic 스키마."""

import uuid
from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel


# ── Activity 응답 ──


class ActivityResponse(BaseModel):
    id: uuid.UUID
    action: str
    actor_id: uuid.UUID
    detail: dict[str, Any] | None = None
    created_at: datetime


class ActivityListResponse(BaseModel):
    items: list[ActivityResponse]
    next_cursor: uuid.UUID | None = None


# ── 타임라인 ──


class TimelineCommentItem(BaseModel):
    type: Literal["comment"] = "comment"
    id: uuid.UUID
    body: str
    author_id: uuid.UUID | None = None
    created_at: datetime


class TimelineActivityItem(BaseModel):
    type: Literal["activity"] = "activity"
    id: uuid.UUID
    action: str
    actor_id: uuid.UUID
    detail: dict[str, Any] | None = None
    created_at: datetime


class TimelineResponse(BaseModel):
    items: list[TimelineCommentItem | TimelineActivityItem]
