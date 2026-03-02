"""팀 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.team import repository as repo
from app.modules.team.schemas import TeamListResponse, TeamSummary


@transactional(read_only=True)
def list_teams(
    db: Session,
    auth: AuthContext,
) -> TeamListResponse:
    """Team 목록 조회 — member_count 포함."""
    rows = repo.list_teams(db)

    items = [
        TeamSummary(
            id=team.id,
            name=team.name,
            description=team.description,
            member_count=member_count,
            created_by=team.created_by,
            created_at=team.created_at,
        )
        for team, member_count in rows
    ]

    return TeamListResponse(items=items)
