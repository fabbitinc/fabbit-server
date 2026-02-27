"""프로젝트 도메인 모델."""

import uuid

from sqlalchemy import ForeignKey, Index, Integer, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.aggregate import AggregateRoot
from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, TimestampMixin, UpdatableMixin


class Project(AggregateRoot, AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    __tablename__ = "projects"

    name: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    issue_counter: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    def next_issue_number(self) -> int:
        """이슈 번호 채번 — 카운터를 1 증가시키고 새 번호를 반환한다."""
        self.issue_counter += 1
        return self.issue_counter


class ProjectMember(TimestampMixin, PkMixin, TenantBase):
    """Project ↔ User 멤버 관계 (M:N)."""

    __tablename__ = "project_members"

    __table_args__ = (
        # 동일 Project-User 관계 중복 방지
        UniqueConstraint(
            "project_id",
            "user_id",
            name="uq_project_members_project_id_user_id",
        ),
        # Project 기준 멤버 조회 최적화
        Index("ix_project_members_project_id", "project_id"),
        # User 기준 소속 프로젝트 조회 최적화
        Index("ix_project_members_user_id", "user_id"),
    )

    project_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
        nullable=False,
    )
    # User id 논리적 참조 (cross-schema FK 없음)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )


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
