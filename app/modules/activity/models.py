"""Activity 도메인 모델."""

import uuid
from typing import Any

from sqlalchemy import Enum, Index, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import PkMixin, TimestampMixin

from .constants import TargetType


class Activity(TimestampMixin, PkMixin, TenantBase):
    """활동 이력 — append-only UI 피드용."""

    __tablename__ = "activities"

    __table_args__ = (
        # 대상별 피드 조회 최적화
        Index("ix_activities_target", "target_type", "target_id"),
    )

    target_type: Mapped[TargetType] = mapped_column(
        Enum(TargetType, name="activity_target_type"), nullable=False
    )
    target_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), nullable=False
    )
    action: Mapped[str] = mapped_column(String(50), nullable=False)
    # User id 논리적 참조 (cross-schema FK 없음)
    actor_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), nullable=False
    )
    detail: Mapped[dict[str, Any] | None] = mapped_column(JSONB, nullable=True)
