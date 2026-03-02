"""팀 도메인 모델."""

import uuid

from sqlalchemy import ForeignKey, Index, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import PkMixin, TimestampMixin, UpdatableMixin


class Team(UpdatableMixin, PkMixin, TenantBase):
    """조직 내 멤버 그룹핑 단위."""

    __tablename__ = "teams"

    __table_args__ = (
        # 테넌트 스키마 내 팀명 중복 방지
        UniqueConstraint("name", name="uq_teams_name"),
    )

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    # 팀 생성자 — User id 논리적 참조 (cross-schema FK 없음)
    created_by: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )


class TeamMember(TimestampMixin, PkMixin, TenantBase):
    """Team ↔ User 멤버 관계 (M:N)."""

    __tablename__ = "team_members"

    __table_args__ = (
        # 동일 Team-User 관계 중복 방지
        UniqueConstraint(
            "team_id",
            "user_id",
            name="uq_team_members_team_id_user_id",
        ),
        # Team 기준 멤버 조회 최적화
        Index("ix_team_members_team_id", "team_id"),
        # User 기준 소속 팀 조회 최적화
        Index("ix_team_members_user_id", "user_id"),
    )

    team_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("teams.id", ondelete="CASCADE"),
        nullable=False,
    )
    # User id 논리적 참조 (cross-schema FK 없음)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
