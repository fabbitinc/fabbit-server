"""팀 상세 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.team import repository as repo
from app.modules.team.schemas import TeamDetailResponse


@transactional(read_only=True)
def get_team_detail(
    db: Session, auth: AuthContext, team_id: uuid.UUID
) -> TeamDetailResponse:
    """Team 단건 상세 조회."""
    team = repo.get_by_id(db, team_id)
    if not team:
        raise AppError(
            message=f"Team '{team_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    return TeamDetailResponse(
        id=team.id,
        name=team.name,
        description=team.description,
        member_count=repo.count_members(db, team_id),
        created_by=team.created_by,
        created_at=team.created_at,
        updated_at=team.updated_at,
    )
