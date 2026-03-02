"""Notification 응답 스키마."""

import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel

from app.modules.user.schemas import UserSummary


class MentionPayload(BaseModel):
    """멘션 알림 상세 데이터."""

    project_id: str
    source_issue_id: str
    source_number: int
    source_title: str
    source_issue_type: str
    is_comment: bool


class NotificationResponse(BaseModel):
    """알림 단건 응답."""

    model_config = {"from_attributes": True}

    id: uuid.UUID
    type: str
    actor_id: uuid.UUID
    payload: MentionPayload | dict[str, Any]
    read_at: datetime | None
    created_at: datetime


class NotificationListResponse(BaseModel):
    """알림 목록 응답 (cursor 페이지네이션)."""

    items: list[NotificationResponse]
    next_cursor: uuid.UUID | None
    users: dict[str, UserSummary]


class UnreadCountResponse(BaseModel):
    """미읽음 개수 응답."""

    count: int
