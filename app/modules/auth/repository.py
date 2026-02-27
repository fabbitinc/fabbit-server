"""인증/조직 데이터 접근."""

import uuid
from datetime import datetime

from sqlalchemy import select, delete, update
from sqlalchemy.orm import Session
from sqlalchemy.sql import func

from app.modules.auth.models import Membership, Organization, RefreshToken, User


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
