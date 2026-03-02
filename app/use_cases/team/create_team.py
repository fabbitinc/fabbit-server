"""팀 생성."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.team import service as team_service
from app.modules.team.models import Team


@transactional()
def create_team(
    db: Session,
    auth: AuthContext,
    name: str,
    description: str | None = None,
) -> Team:
    """팀 생성 — 빈 멤버"""
    return team_service.create_team(
        db, name=name, description=description, created_by=auth.user_id
    )
