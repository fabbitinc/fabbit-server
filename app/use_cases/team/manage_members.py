"""팀 멤버 추가/제거."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.team import service as team_service
from app.modules.team.schemas import ManageTeamMembersResponse


@transactional()
def add_members(
    db: Session,
    auth: AuthContext,
    team_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> ManageTeamMembersResponse:
    """팀에 멤버 배치 추가."""
    team_service.get_or_raise(db, team_id)
    count = team_service.add_members(db, team_id, user_ids)
    return ManageTeamMembersResponse(count=count)


@transactional()
def remove_members(
    db: Session,
    auth: AuthContext,
    team_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> ManageTeamMembersResponse:
    """팀에서 멤버 배치 제거."""
    team_service.get_or_raise(db, team_id)
    count = team_service.remove_members(db, team_id, user_ids)
    return ManageTeamMembersResponse(count=count)
