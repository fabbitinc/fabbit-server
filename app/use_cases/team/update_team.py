"""팀 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.team import service as team_service
from app.modules.team.models import Team


@transactional()
def update_team(
    db: Session,
    auth: AuthContext,
    team_id: uuid.UUID,
    name: str | None = None,
    description: str | None = None,
) -> Team:
    """팀 정보 수정 — 변경된 필드만 반영."""
    team = team_service.get_or_raise(db, team_id)
    team_service.update_team(db, team, name=name, description=description)
    return team
