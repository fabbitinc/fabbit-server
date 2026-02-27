"""인증/조직 ORM 모델 (public 스키마)."""

import hashlib
import secrets
import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func
from app.core.config import settings
from app.core.database import Base, generate_uuid7
from sqlalchemy import Integer
from app.modules.auth.constants import EmailVerificationStatus, InvitationStatus, MembershipRole


def _hash_token(token: str) -> str:
    """초대 토큰을 SHA-256 해시로 변환. DB에는 해시만 저장."""
    return hashlib.sha256(token.encode()).hexdigest()


class User(Base):
    __tablename__ = "users"

    __table_args__ = (
        # 이메일 유일성
        UniqueConstraint("email", name="uq_users_email"),
    )

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


class Organization(Base):
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
    onboarded_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
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
    role: Mapped[str] = mapped_column(String(20), nullable=False, default=MembershipRole.MEMBER)
    job_role: Mapped[str | None] = mapped_column(String(50), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    user: Mapped["User"] = relationship("User", back_populates="memberships")
    organization: Mapped["Organization"] = relationship(
        "Organization", back_populates="memberships"
    )


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    __table_args__ = (
        # jti 유일성
        UniqueConstraint("token_jti", name="uq_refresh_tokens_token_jti"),
        # 유저별 토큰 조회 최적화
        Index("ix_refresh_tokens_user_id", "user_id"),
    )

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


class Invitation(Base):
    __tablename__ = "invitations"

    __table_args__ = (
        # 조직-이메일 조합 유일성 (중복 초대 방지)
        UniqueConstraint("org_id", "email", name="uq_invitations_org_id_email"),
        # 토큰 해시 유일성 (수락 시 조회)
        UniqueConstraint("token_hash", name="uq_invitations_token_hash"),
        # 조직별 초대 목록 조회 최적화
        Index("ix_invitations_org_id", "org_id"),
        # 초대자별 조회 최적화
        Index("ix_invitations_invited_by", "invited_by"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    org_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("organizations.id", ondelete="CASCADE"),
        nullable=False,
    )
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(
        String(20), nullable=False, default=MembershipRole.MEMBER
    )
    # SHA-256 해시된 토큰 (DB에 평문 저장하지 않음)
    token_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=InvitationStatus.PENDING
    )
    invited_by: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    accepted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    organization: Mapped["Organization"] = relationship("Organization")
    inviter: Mapped["User"] = relationship("User")

    @classmethod
    def create(
        cls,
        org_id: uuid.UUID,
        email: str,
        invited_by: uuid.UUID,
        role: str = MembershipRole.MEMBER,
    ) -> tuple["Invitation", str]:
        """초대 생성 팩토리.

        Returns:
            (Invitation 인스턴스, 원본 토큰) — 원본 토큰은 이메일 발송에만 사용하고 DB에 저장하지 않는다.
        """
        raw_token = secrets.token_urlsafe(32)
        invitation = cls(
            org_id=org_id,
            email=email,
            role=role,
            token_hash=_hash_token(raw_token),
            status=InvitationStatus.PENDING,
            invited_by=invited_by,
            expires_at=datetime.now(timezone.utc)
            + timedelta(days=settings.invitation_expire_days),
        )
        return invitation, raw_token

    def cancel(self) -> None:
        """초대 취소."""
        self.status = InvitationStatus.CANCELLED

    def accept(self) -> None:
        """초대 수락."""
        self.status = InvitationStatus.ACCEPTED
        self.accepted_at = datetime.now(timezone.utc)

    @property
    def is_expired(self) -> bool:
        return datetime.now(timezone.utc) > self.expires_at


class EmailVerification(Base):
    """이메일 인증코드 (회원가입 전 이메일 소유권 검증)."""

    __tablename__ = "email_verifications"

    __table_args__ = (
        # 이메일별 인증코드 조회 최적화
        Index("ix_email_verifications_email", "email"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    # SHA-256 해시된 6자리 코드
    code_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    # SHA-256 해시된 verification_token (verify 성공 시 생성)
    verification_token_hash: Mapped[str | None] = mapped_column(
        String(64), nullable=True
    )
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=EmailVerificationStatus.PENDING
    )
    attempt_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    @classmethod
    def create(cls, email: str) -> tuple["EmailVerification", str]:
        """인증코드 생성 팩토리.

        Returns:
            (EmailVerification 인스턴스, 6자리 원본 코드) — 원본 코드는 이메일 발송에만 사용.
        """
        code = f"{secrets.randbelow(1_000_000):06d}"
        return cls(
            email=email,
            code_hash=_hash_token(code),
            status=EmailVerificationStatus.PENDING,
            attempt_count=0,
            expires_at=datetime.now(timezone.utc)
            + timedelta(minutes=settings.email_verification_expire_minutes),
        ), code

    def verify(self) -> str:
        """코드 검증 성공 처리 → VERIFIED + verification_token 생성.

        Returns:
            원본 verification_token — 클라이언트에 반환 후 DB에는 해시만 유지.
        """
        self.status = EmailVerificationStatus.VERIFIED
        raw_token = secrets.token_urlsafe(32)
        self.verification_token_hash = _hash_token(raw_token)
        return raw_token

    def use(self) -> None:
        """가입 완료 처리 → USED."""
        self.status = EmailVerificationStatus.USED

    def increment_attempt(self) -> None:
        """검증 시도 횟수 증가."""
        self.attempt_count += 1

    @property
    def is_expired(self) -> bool:
        return datetime.now(timezone.utc) > self.expires_at

    @property
    def is_max_attempts(self) -> bool:
        return self.attempt_count >= settings.email_verification_max_attempts
