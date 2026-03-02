"""팀 삭제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.team import service as team_service


@transactional()
def delete_team(
    db: Session, auth: AuthContext, team_id: uuid.UUID
) -> None:
    """팀 삭제."""
    team_service.get_or_raise(db, team_id)
    team_service.delete_team(db, team_id)
