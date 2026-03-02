"""팀(Team) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.team import repository as repo
from app.modules.team.models import Team


def get_or_raise(db: Session, team_id: uuid.UUID) -> Team:
    """Team 조회 — 없으면 AppError(NOT_FOUND)."""
    team = repo.get_by_id(db, team_id)
    if not team:
        raise AppError(
            message=f"Team '{team_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )
    return team


def create_team(
    db: Session,
    name: str,
    description: str | None = None,
    created_by: uuid.UUID | None = None,
) -> Team:
    """팀 생성 — created_by가 주어지면 자동 멤버 등록."""
    team = Team(name=name, description=description, created_by=created_by)
    repo.add(db, team)
    return team


def update_team(
    db: Session,
    team: Team,
    name: str | None = None,
    description: str | None = None,
) -> Team:
    """팀 정보 수정 — 변경된 필드만 반영."""
    if name is not None and name != team.name:
        team.name = name
    if description is not None and description != team.description:
        team.description = description
    return team


def delete_team(db: Session, team_id: uuid.UUID) -> None:
    """팀 삭제."""
    repo.delete(db, team_id)


def add_members(
    db: Session,
    team_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> int:
    """Team에 멤버 배치 추가 — 신규 추가 건수 반환."""
    return repo.add_members(db, team_id, user_ids)


def remove_members(db: Session, team_id: uuid.UUID, user_ids: list[uuid.UUID]) -> int:
    """Team에서 멤버 배치 제거 — 삭제 건수 반환."""
    return repo.remove_members(db, team_id, user_ids)
