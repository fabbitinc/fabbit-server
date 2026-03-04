"""조직/멤버십 데이터 접근."""

import uuid

from sqlalchemy import case, delete, func, select, update
from sqlalchemy.orm import Session

from app.modules.organization.constants import MembershipRole
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
    max_members: int = 0,
    plan_credits_remaining: int = 0,
    storage_bytes_limit: int = 0,
) -> Organization:
    org = Organization(
        slug=slug, name=name, owner_id=owner_id,
        industry=industry, team_size=team_size, plan_type=plan_type,
        max_members=max_members,
        plan_credits_remaining=plan_credits_remaining,
        storage_bytes_limit=storage_bytes_limit,
    )
    db.add(org)
    db.flush()
    return org


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
    role_order = case(
        (Membership.role == MembershipRole.OWNER, 0),
        (Membership.role == MembershipRole.ADMIN, 1),
        else_=2,
    )
    return list(
        db.execute(
            select(User, Membership)
            .join(Membership, User.id == Membership.user_id)
            .where(Membership.org_id == org_id)
            .order_by(role_order, User.full_name)
        ).all()
    )


def lookup_members(
    db: Session,
    org_id: uuid.UUID,
    *,
    search: str | None = None,
    limit: int = 10,
) -> list[User]:
    """조직 멤버 lookup 조회 (picker/autocomplete용)."""
    query = (
        db.query(User)
        .join(Membership, User.id == Membership.user_id)
        .filter(Membership.org_id == org_id)
    )
    if search:
        query = query.filter(User.full_name.ilike(f"%{search}%"))
    return query.order_by(User.full_name).limit(limit).all()


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


def count_members(db: Session, org_id: uuid.UUID) -> int:
    """조직의 전체 멤버 수 조회."""
    return db.scalar(
        select(func.count()).where(Membership.org_id == org_id)
    ) or 0


def count_owners(db: Session, org_id: uuid.UUID) -> int:
    """조직의 OWNER 역할 멤버 수 조회."""
    return db.scalar(
        select(func.count()).where(
            Membership.org_id == org_id,
            Membership.role == MembershipRole.OWNER,
        )
    ) or 0


# ── User 조회 (MeResponse 조립용, cross-domain JOIN 허용) ──


def get_user_by_id(db: Session, user_id: uuid.UUID) -> User | None:
    """MeResponse 조립용 User 조회."""
    return db.get(User, user_id)


# ── 쿼타 원자적 연산 ──


def reserve_member_seat(db: Session, org_id: uuid.UUID) -> bool:
    """멤버 좌석 예약. max_members=-1이면 무제한. rowcount 0이면 한도 초과."""
    result = db.execute(
        update(Organization)
        .where(
            Organization.id == org_id,
            (Organization.max_members == -1) | (Organization.used_members < Organization.max_members),
        )
        .values(used_members=Organization.used_members + 1)
    )
    db.flush()
    return result.rowcount > 0


def release_member_seat(db: Session, org_id: uuid.UUID) -> None:
    """멤버 좌석 반환."""
    db.execute(
        update(Organization)
        .where(Organization.id == org_id)
        .values(used_members=Organization.used_members - 1)
    )
    db.flush()


def consume_credits(db: Session, org_id: uuid.UUID, cost: int) -> bool:
    """크레딧 소비. plan 우선 차감 → 부족분 bonus 차감. rowcount 0이면 잔액 부족."""
    result = db.execute(
        update(Organization)
        .where(
            Organization.id == org_id,
            Organization.plan_credits_remaining + Organization.bonus_credits_remaining >= cost,
        )
        .values(
            plan_credits_remaining=func.greatest(Organization.plan_credits_remaining - cost, 0),
            bonus_credits_remaining=Organization.bonus_credits_remaining
            - func.greatest(cost - Organization.plan_credits_remaining, 0),
        )
    )
    db.flush()
    return result.rowcount > 0


def consume_storage_bytes(db: Session, org_id: uuid.UUID, delta_bytes: int) -> bool:
    """스토리지 소비. allow_storage_overage=true면 한도 무시. rowcount 0이면 한도 초과."""
    result = db.execute(
        update(Organization)
        .where(
            Organization.id == org_id,
            (Organization.allow_storage_overage == True)  # noqa: E712
            | (Organization.storage_bytes_used + delta_bytes <= Organization.storage_bytes_limit),
        )
        .values(storage_bytes_used=Organization.storage_bytes_used + delta_bytes)
    )
    db.flush()
    return result.rowcount > 0


def release_storage_bytes(db: Session, org_id: uuid.UUID, delta_bytes: int) -> None:
    """스토리지 반환 (파일 삭제 시)."""
    db.execute(
        update(Organization)
        .where(Organization.id == org_id)
        .values(storage_bytes_used=Organization.storage_bytes_used - delta_bytes)
    )
    db.flush()
