"""인증/조직 데이터 접근."""

import uuid
from datetime import datetime

from sqlalchemy import select, delete
from sqlalchemy.orm import Session

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
    db: Session, slug: str, name: str, owner_id: uuid.UUID
) -> Organization:
    org = Organization(slug=slug, name=name, owner_id=owner_id)
    db.add(org)
    db.flush()
    return org


# ── Membership ──


def create_membership(
    db: Session, user_id: uuid.UUID, org_id: uuid.UUID, role: str = "ADMIN"
) -> Membership:
    membership = Membership(user_id=user_id, org_id=org_id, role=role)
    db.add(membership)
    db.flush()
    return membership


def get_user_memberships(db: Session, user_id: uuid.UUID) -> list[Membership]:
    return list(
        db.scalars(
            select(Membership).where(Membership.user_id == user_id)
        ).all()
    )


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
