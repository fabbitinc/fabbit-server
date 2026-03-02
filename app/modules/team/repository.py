"""팀(Team) 도메인 Repository."""

import uuid

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.modules.team.models import Team, TeamMember


def add(db: Session, entity: Team) -> Team:
    """Team 저장."""
    db.add(entity)
    db.flush()
    return entity


def get_by_id(db: Session, team_id: uuid.UUID) -> Team | None:
    """Team 단건 조회."""
    return db.query(Team).filter(Team.id == team_id).first()


def list_teams(db: Session) -> list[tuple[Team, int]]:
    """Team 목록 조회 — member_count 포함."""
    member_count = (
        select(func.count(TeamMember.id))
        .where(TeamMember.team_id == Team.id)
        .correlate(Team)
        .scalar_subquery()
        .label("member_count")
    )
    return db.query(Team, member_count).order_by(Team.name).all()


def delete(db: Session, team_id: uuid.UUID) -> None:
    """Team 삭제 — CASCADE로 TeamMember도 함께 삭제."""
    db.query(Team).filter(Team.id == team_id).delete(synchronize_session="fetch")
    db.flush()


def add_members(
    db: Session,
    team_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> int:
    """멤버 배치 추가 — 중복 무시, 신규 건수 반환."""
    existing = set(
        row[0]
        for row in db.query(TeamMember.user_id)
        .filter(
            TeamMember.team_id == team_id,
            TeamMember.user_id.in_(user_ids),
        )
        .all()
    )
    new_ids = [uid for uid in user_ids if uid not in existing]
    for uid in new_ids:
        db.add(TeamMember(team_id=team_id, user_id=uid))
    if new_ids:
        db.flush()
    return len(new_ids)


def remove_members(db: Session, team_id: uuid.UUID, user_ids: list[uuid.UUID]) -> int:
    """멤버 배치 제거 — 삭제 건수 반환."""
    count = (
        db.query(TeamMember)
        .filter(
            TeamMember.team_id == team_id,
            TeamMember.user_id.in_(user_ids),
        )
        .delete(synchronize_session="fetch")
    )
    db.flush()
    return count


def list_members(db: Session, team_id: uuid.UUID) -> list[TeamMember]:
    """Team 멤버 목록 조회."""
    return (
        db.query(TeamMember)
        .filter(TeamMember.team_id == team_id)
        .all()
    )


def lookup_teams(
    db: Session,
    *,
    search: str | None = None,
    limit: int = 10,
) -> list[Team]:
    """팀 lookup 조회 (picker/autocomplete용)."""
    query = db.query(Team)
    if search:
        query = query.filter(Team.name.ilike(f"%{search}%"))
    return query.order_by(Team.name).limit(limit).all()


def count_members(db: Session, team_id: uuid.UUID) -> int:
    """Team 멤버 수 조회."""
    return (
        db.query(func.count(TeamMember.id))
        .filter(TeamMember.team_id == team_id)
        .scalar()
    )
