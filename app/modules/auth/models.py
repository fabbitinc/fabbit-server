"""인증 ORM 모델 (public 스키마) — RefreshToken, Invitation, EmailVerification."""

from __future__ import annotations

import hashlib
import secrets
import uuid
from datetime import datetime, timedelta, timezone
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.config import settings
from app.core.database import Base, generate_uuid7
from app.modules.auth.constants import EmailVerificationStatus, InvitationStatus
from app.modules.organization.constants import MembershipRole

if TYPE_CHECKING:
    from app.modules.organization.models import Organization
    from app.modules.user.models import User


def _hash_token(token: str) -> str:
    """초대 토큰을 SHA-256 해시로 변환. DB에는 해시만 저장."""
    return hashlib.sha256(token.encode()).hexdigest()


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
