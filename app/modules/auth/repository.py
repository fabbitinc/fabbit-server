"""인증 전용 데이터 접근 — RefreshToken, Invitation, EmailVerification."""

import uuid
from datetime import datetime

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.modules.auth.constants import EmailVerificationStatus, InvitationStatus
from app.modules.auth.models import EmailVerification, Invitation, RefreshToken
from app.modules.user.models import User


# ── RefreshToken ──


def save_refresh_token(
    db: Session, user_id: uuid.UUID, token_jti: str, expires_at: datetime
) -> RefreshToken:
    rt = RefreshToken(user_id=user_id, token_jti=token_jti, expires_at=expires_at)
    db.add(rt)
    db.flush()
    return rt


def get_refresh_token_by_jti(db: Session, token_jti: str) -> RefreshToken | None:
    return db.scalars(
        select(RefreshToken).where(RefreshToken.token_jti == token_jti)
    ).first()


def delete_refresh_token_by_jti(db: Session, token_jti: str) -> None:
    db.execute(delete(RefreshToken).where(RefreshToken.token_jti == token_jti))


def delete_all_user_refresh_tokens(db: Session, user_id: uuid.UUID) -> None:
    """유저의 모든 리프레시 토큰 폐기 (토큰 재사용 감지 시)."""
    db.execute(delete(RefreshToken).where(RefreshToken.user_id == user_id))


# ── Invitation ──


def create_invitation(db: Session, invitation: Invitation) -> Invitation:
    db.add(invitation)
    db.flush()
    return invitation


def get_invitation_by_token_hash(db: Session, token_hash: str) -> Invitation | None:
    return db.scalars(
        select(Invitation).where(Invitation.token_hash == token_hash)
    ).first()


def get_invitation_by_id(db: Session, invitation_id: uuid.UUID) -> Invitation | None:
    return db.get(Invitation, invitation_id)


def get_pending_invitation(
    db: Session, org_id: uuid.UUID, email: str
) -> Invitation | None:
    """조직-이메일 조합의 PENDING 초대 조회."""
    return db.scalars(
        select(Invitation).where(
            Invitation.org_id == org_id,
            Invitation.email == email,
            Invitation.status == InvitationStatus.PENDING,
        )
    ).first()


def list_invitations_by_org(db: Session, org_id: uuid.UUID) -> list[Invitation]:
    """조직의 초대 목록 조회 (최신순)."""
    return list(
        db.scalars(
            select(Invitation)
            .where(Invitation.org_id == org_id)
            .order_by(Invitation.created_at.desc())
        ).all()
    )


def delete_invitation_by_org_email(
    db: Session, org_id: uuid.UUID, email: str
) -> None:
    """재초대 시 기존 CANCELLED 레코드 삭제."""
    db.execute(
        delete(Invitation).where(
            Invitation.org_id == org_id,
            Invitation.email == email,
            Invitation.status == InvitationStatus.CANCELLED,
        )
    )


# ── EmailVerification ──


def create_email_verification(
    db: Session, verification: EmailVerification
) -> EmailVerification:
    db.add(verification)
    db.flush()
    return verification


def get_pending_verification_by_email(
    db: Session, email: str
) -> EmailVerification | None:
    """이메일의 최신 PENDING 인증코드 조회 (쿨다운 체크용)."""
    return db.scalars(
        select(EmailVerification)
        .where(
            EmailVerification.email == email,
            EmailVerification.status == EmailVerificationStatus.PENDING,
        )
        .order_by(EmailVerification.created_at.desc())
        .limit(1)
    ).first()


def get_pending_verification_by_email_and_code_hash(
    db: Session, email: str, code_hash: str
) -> EmailVerification | None:
    """이메일 + 코드 해시로 PENDING 인증코드 조회 (verify 단계)."""
    return db.scalars(
        select(EmailVerification).where(
            EmailVerification.email == email,
            EmailVerification.code_hash == code_hash,
            EmailVerification.status == EmailVerificationStatus.PENDING,
        )
    ).first()


def get_verified_by_token_hash_and_code_hash(
    db: Session, token_hash: str, code_hash: str
) -> EmailVerification | None:
    """verification_token_hash + code_hash로 VERIFIED 인증 조회 (register 단계)."""
    return db.scalars(
        select(EmailVerification).where(
            EmailVerification.verification_token_hash == token_hash,
            EmailVerification.code_hash == code_hash,
            EmailVerification.status == EmailVerificationStatus.VERIFIED,
        )
    ).first()


def delete_pending_verifications_by_email(db: Session, email: str) -> None:
    """재발송 시 이전 PENDING 인증코드 삭제."""
    db.execute(
        delete(EmailVerification).where(
            EmailVerification.email == email,
            EmailVerification.status == EmailVerificationStatus.PENDING,
        )
    )


# ── User 조회 (check_email/send_verification용, cross-domain 모델 허용) ──


def exists_user_by_email(db: Session, email: str) -> bool:
    """이메일로 사용자 존재 여부 확인."""
    return db.scalars(select(User).where(User.email == email)).first() is not None
