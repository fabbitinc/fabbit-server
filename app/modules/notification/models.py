"""Notification 도메인 모델."""

import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import DateTime, Enum, Index
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import PkMixin, TimestampMixin

from .constants import NotificationType


class Notification(TimestampMixin, PkMixin, TenantBase):
    """알림 — append-only 피드. read_at만 갱신."""

    __tablename__ = "notifications"

    __table_args__ = (
        # 수신자별 미읽음 조회 최적화
        Index("ix_notifications_user_unread", "user_id", "read_at"),
    )

    # 수신자 (논리적 참조, cross-schema FK 없음)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), nullable=False
    )
    type: Mapped[NotificationType] = mapped_column(
        Enum(NotificationType, name="notification_type"), nullable=False
    )
    # 발행자 (논리적 참조)
    actor_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), nullable=False
    )
    # 타입별 상세 데이터
    payload: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    # NULL = 미읽음
    read_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
