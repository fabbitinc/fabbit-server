"""조직/멤버십 ORM 모델 (public 스키마)."""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import (
    Boolean,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.aggregate import AggregateRoot
from app.core.database import Base, generate_uuid7
from app.modules.file.events import FileAttached, FileDetached
from app.modules.organization.constants import MembershipRole

if TYPE_CHECKING:
    from app.modules.file.models import File
    from app.modules.user.models import User


class Organization(AggregateRoot, Base):
    __tablename__ = "organizations"

    __table_args__ = (
        # 슬러그 유일성
        UniqueConstraint("slug", name="uq_organizations_slug"),
        # 소유자별 조직 조회 최적화
        Index("ix_organizations_owner_id", "owner_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    slug: Mapped[str] = mapped_column(String(50), nullable=False)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    owner_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    industry: Mapped[str | None] = mapped_column(String(50), nullable=True)
    team_size: Mapped[str | None] = mapped_column(String(20), nullable=True)
    plan_type: Mapped[str] = mapped_column(
        String(20), nullable=False, default="STARTER"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    profile_image_file_key: Mapped[str | None] = mapped_column(
        String(1000), nullable=True
    )

    # ── 실행 상태 (쿼타/잔량) ──

    max_members: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )  # 캐시 — 플랜 변경 시 동기화. -1=무제한
    used_members: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )  # SSoT — 원자적 증감
    plan_credits_remaining: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )  # 매 빌링 기간 리셋
    bonus_credits_remaining: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )  # 추가 구매분. 감소만, 리셋 없음
    storage_mb_limit: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )  # 캐시 — 플랜 기본 + 추가분
    storage_mb_used: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )  # 누적 상태, 파일 업로드/삭제 시 증감
    allow_storage_overage: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False
    )  # true면 한도 초과 허용(과금)

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    memberships: Mapped[list["Membership"]] = relationship(
        "Membership", back_populates="organization"
    )

    # ── 도메인 메서드 ──

    def set_profile_image(self, file: "File") -> None:
        """프로필 이미지 설정 — 소유자 할당은 FileHandler가 처리."""
        self.profile_image_file_key = file.file_key
        self.register_event(
            FileAttached(
                owner_type="organization",
                owner_id=self.id,
                file_ids=[file.id],
            )
        )

    def remove_profile_image(self, file_id: uuid.UUID) -> None:
        """프로필 이미지 제거 — 소프트 삭제는 FileHandler가 처리."""
        self.profile_image_file_key = None
        self.register_event(
            FileDetached(
                owner_type="organization",
                owner_id=self.id,
                file_id=file_id,
            )
        )


class Membership(Base):
    __tablename__ = "memberships"

    __table_args__ = (
        # 유저-조직 조합 유일성 (user_id가 선두 → 유저별 멤버십 조회 커버)
        UniqueConstraint("user_id", "org_id", name="uq_memberships_user_id_org_id"),
        # 조직별 멤버 목록 조회 최적화
        Index("ix_memberships_org_id", "org_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    org_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("organizations.id", ondelete="CASCADE"),
        nullable=False,
    )
    role: Mapped[str] = mapped_column(
        String(20), nullable=False, default=MembershipRole.MEMBER
    )
    job_role: Mapped[str | None] = mapped_column(String(50), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    user: Mapped["User"] = relationship("User", back_populates="memberships")
    organization: Mapped["Organization"] = relationship(
        "Organization", back_populates="memberships"
    )
