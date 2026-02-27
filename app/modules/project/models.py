"""프로젝트 도메인 모델."""

import uuid

from sqlalchemy import ForeignKey, Index, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.aggregate import AggregateRoot
from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, TimestampMixin, UpdatableMixin


class Project(AggregateRoot, AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    __tablename__ = "projects"

    name: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)


class ProjectPart(TimestampMixin, PkMixin, TenantBase):
    """Project → Part 소속 관계 (M:N)."""

    __tablename__ = "project_parts"

    __table_args__ = (
        # 동일 Project-Part 관계 중복 방지
        UniqueConstraint(
            "project_id",
            "part_id",
            name="uq_project_parts_project_id_part_id",
        ),
        # Project 기준 Part 조회 최적화
        Index("ix_project_parts_project_id", "project_id"),
        # Part 기준 Project 조회 최적화 (역추적)
        Index("ix_project_parts_part_id", "part_id"),
    )

    project_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
        nullable=False,
    )
    part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )
