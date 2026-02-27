"""인증/조직 데이터 접근."""

import uuid
from datetime import datetime

from sqlalchemy import select, delete, update
from sqlalchemy.orm import Session
from sqlalchemy.sql import func

from app.modules.auth.constants import EmailVerificationStatus, InvitationStatus
from app.modules.auth.models import EmailVerification, Invitation, Membership, Organization, RefreshToken, User


# ── User ──


def get_user_by_email(db: Session, email: str) -> User | None:
    return db.scalars(select(User).where(User.email == email)).first()


def get_user_by_id(db: Session, user_id: uuid.UUID) -> User | None:
    return db.get(User, user_id)


def create_user(
    db: Session, email: str, hashed_password: str, full_name: str
) -> User:
    user = User(email=email, hashed_password=hashed_password, full_name=full_name)
    db.add(user)
    db.flush()
    return user


# ── Organization ──


def get_org_by_slug(db: Session, slug: str) -> Organization | None:
    return db.scalars(
        select(Organization).where(Organization.slug == slug)
    ).first()


def create_organization(
    db: Session,
    slug: str,
    name: str,
    owner_id: uuid.UUID,
    *,
    industry: str | None = None,
    team_size: str | None = None,
    plan_type: str = "STARTER",
) -> Organization:
    org = Organization(
        slug=slug, name=name, owner_id=owner_id,
        industry=industry, team_size=team_size, plan_type=plan_type,
    )
    db.add(org)
    db.flush()
    return org


def get_org_by_id(db: Session, org_id: uuid.UUID) -> Organization | None:
    return db.get(Organization, org_id)


def complete_onboarding(db: Session, org_id: uuid.UUID) -> Organization:
    """온보딩 완료 시각 기록."""
    db.execute(
        update(Organization)
        .where(Organization.id == org_id)
        .values(onboarded_at=func.now())
    )
    db.flush()
    return db.get(Organization, org_id)


# ── Membership ──


def create_membership(
    db: Session,
    user_id: uuid.UUID,
    org_id: uuid.UUID,
    role: str = "MEMBER",
    *,
    job_role: str | None = None,
) -> Membership:
    membership = Membership(user_id=user_id, org_id=org_id, role=role, job_role=job_role)
    db.add(membership)
    db.flush()
    return membership


def get_user_memberships(db: Session, user_id: uuid.UUID) -> list[Membership]:
    return list(
        db.scalars(
            select(Membership).where(Membership.user_id == user_id)
        ).all()
    )


def list_org_members(
    db: Session, org_id: uuid.UUID
) -> list[tuple[User, Membership]]:
    """조직 멤버 목록 조회 (User JOIN Membership)."""
    return list(
        db.execute(
            select(User, Membership)
            .join(Membership, User.id == Membership.user_id)
            .where(Membership.org_id == org_id)
            .order_by(User.full_name)
        ).all()
    )


def get_membership_by_slug(
    db: Session, user_id: uuid.UUID, slug: str
) -> Membership | None:
    """유저의 특정 slug 조직 멤버십 조회."""
    return db.scalars(
        select(Membership)
        .join(Organization, Membership.org_id == Organization.id)
        .where(Membership.user_id == user_id, Organization.slug == slug)
    ).first()


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


def get_membership(
    db: Session, user_id: uuid.UUID, org_id: uuid.UUID
) -> Membership | None:
    """유저-조직 멤버십 단건 조회."""
    return db.scalars(
        select(Membership).where(
            Membership.user_id == user_id,
            Membership.org_id == org_id,
        )
    ).first()


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
