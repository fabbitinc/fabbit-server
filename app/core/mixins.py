"""SQLAlchemy 모델용 공통 mixin."""

import uuid
from datetime import datetime, timezone

from sqlalchemy import DateTime
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import generate_uuid7


class PkMixin:
    """UUID v7 PK — 모든 모델에 적용."""

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )


class TimestampMixin:
    """생성 시각 — 모든 모델에 적용."""

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )


class UpdatableMixin(TimestampMixin):
    """생성 + 수정 시각 — 변경 가능 엔티티에 적용."""

    updated_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), onupdate=func.now(), nullable=True
    )


class SoftDeleteMixin:
    """소프트 삭제 — deleted_at 컬럼 + soft_delete() 메서드."""

    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    def soft_delete(self) -> None:
        self.deleted_at = datetime.now(timezone.utc)

    @property
    def is_deleted(self) -> bool:
        return self.deleted_at is not None
