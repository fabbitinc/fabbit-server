"""조직/멤버십 데이터 접근."""

import uuid

from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session
from sqlalchemy.sql import func

from app.modules.organization.models import Membership, Organization
from app.modules.user.models import User


# ── Organization ──


def get_org_by_slug(db: Session, slug: str) -> Organization | None:
    return db.scalars(
        select(Organization).where(Organization.slug == slug)
    ).first()


def get_org_by_id(db: Session, org_id: uuid.UUID) -> Organization | None:
    return db.get(Organization, org_id)


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


def get_first_membership_or_none(db: Session, user_id: uuid.UUID) -> Membership | None:
    """유저의 첫 번째 멤버십 조회 (refresh 토큰 발급용)."""
    return db.scalars(
        select(Membership).where(Membership.user_id == user_id).limit(1)
    ).first()


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


def delete_membership(db: Session, org_id: uuid.UUID, user_id: uuid.UUID) -> None:
    """조직에서 멤버 제거."""
    db.execute(
        delete(Membership).where(
            Membership.org_id == org_id,
            Membership.user_id == user_id,
        )
    )
    db.flush()


# ── User 조회 (MeResponse 조립용, cross-domain JOIN 허용) ──


def get_user_by_id(db: Session, user_id: uuid.UUID) -> User | None:
    """MeResponse 조립용 User 조회."""
    return db.get(User, user_id)
