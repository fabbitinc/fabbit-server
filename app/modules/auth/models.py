"""인증/조직 ORM 모델 (public 스키마)."""

import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func
from app.core.database import Base, generate_uuid7


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(100), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    memberships: Mapped[list["Membership"]] = relationship(
        "Membership", back_populates="user"
    )

    __table_args__ = (
        # 이메일 유일성
        UniqueConstraint("email", name="uq_users_email"),
    )


class Organization(Base):
    __tablename__ = "organizations"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    slug: Mapped[str] = mapped_column(String(50), nullable=False)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    owner_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )
    industry: Mapped[str | None] = mapped_column(String(50), nullable=True)
    team_size: Mapped[str | None] = mapped_column(String(20), nullable=True)
    plan_type: Mapped[str] = mapped_column(String(20), nullable=False, default="FREE")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    memberships: Mapped[list["Membership"]] = relationship(
        "Membership", back_populates="organization"
    )

    __table_args__ = (
        # 슬러그 유일성
        UniqueConstraint("slug", name="uq_organizations_slug"),
        # 소유자별 조직 조회 최적화
        Index("ix_organizations_owner_id", "owner_id"),
    )


class Membership(Base):
    __tablename__ = "memberships"

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
    role: Mapped[str] = mapped_column(String(20), nullable=False, default="MEMBER")
    job_role: Mapped[str | None] = mapped_column(String(50), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    user: Mapped["User"] = relationship("User", back_populates="memberships")
    organization: Mapped["Organization"] = relationship(
        "Organization", back_populates="memberships"
    )

    __table_args__ = (
        # 유저-조직 조합 유일성 (user_id가 선두 → 유저별 멤버십 조회 커버)
        UniqueConstraint("user_id", "org_id", name="uq_memberships_user_org"),
        # 조직별 멤버 목록 조회 최적화
        Index("ix_memberships_org_id", "org_id"),
    )


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    token_jti: Mapped[str] = mapped_column(String(36), nullable=False)
    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    __table_args__ = (
        # jti 유일성
        UniqueConstraint("token_jti", name="uq_refresh_tokens_token_jti"),
        # 유저별 토큰 조회 최적화
        Index("ix_refresh_tokens_user_id", "user_id"),
    )
