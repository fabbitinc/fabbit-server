"""SQLAlchemy 모델용 공통 mixin."""

import uuid
from datetime import datetime, timezone

from sqlalchemy import DateTime, event
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, Session, mapped_column, with_loader_criteria
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
        DateTime(timezone=True),
        default=func.now(),
        server_default=func.now(),
        nullable=False,
    )


class UpdatableMixin(TimestampMixin):
    """생성 + 수정 시각 — 변경 가능 엔티티에 적용."""

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=func.now(),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    @property
    def is_modified(self) -> bool:
        return self.updated_at > self.created_at


class AuditMixin:
    """생성자 + 수정자 추적 — 감사 이력이 필요한 엔티티에 적용.

    AuthContext.user_id를 논리적 참조로 저장 (cross-schema FK 없음).
    session.info["user_id"] 설정 시 before_flush에서 자동 할당.
    """

    created_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True
    )
    updated_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True
    )


@event.listens_for(Session, "before_flush")
def _auto_audit(session: Session, flush_context, instances) -> None:
    """AuditMixin 인스턴스의 created_by/updated_by를 자동 할당."""
    user_id = session.info.get("user_id")
    if not user_id:
        return
    for obj in session.new:
        if isinstance(obj, AuditMixin):
            obj.created_by = user_id
            obj.updated_by = user_id
    for obj in session.dirty:
        if isinstance(obj, AuditMixin):
            obj.updated_by = user_id


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


@event.listens_for(Session, "do_orm_execute")
def _apply_soft_delete_filter(execute_state) -> None:
    """SoftDeleteMixin 상속 모델의 SELECT 쿼리에 deleted_at IS NULL 자동 적용.

    우회: query.execution_options(include_deleted=True)
    """
    if execute_state.is_select and not execute_state.execution_options.get(
        "include_deleted", False
    ):
        execute_state.statement = execute_state.statement.options(
            with_loader_criteria(
                SoftDeleteMixin,
                lambda cls: cls.deleted_at.is_(None),
                include_aliases=True,
            )
        )
