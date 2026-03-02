"""팀 멤버(Team Member) API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.team.schemas import (
    AddTeamMembersRequest,
    ManageTeamMembersResponse,
    RemoveTeamMembersRequest,
    TeamMemberListResponse,
)
from app.queries import team as team_queries
from app.use_cases import team as team_commands

router = APIRouter(prefix="/api/v1/teams/{team_id}/members", tags=["team-members"])


@router.get("", response_model=TeamMemberListResponse)
def list_team_members(
    team_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 멤버 목록 조회."""
    return team_queries.list_members(db, auth, team_id)


@router.post("", response_model=ManageTeamMembersResponse, status_code=201)
def add_team_members(
    team_id: uuid.UUID,
    req: AddTeamMembersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀에 멤버 배치 추가.

    이미 추가된 멤버는 무시되며, 신규 추가 건수를 반환합니다.
    """
    return team_commands.add_members(db, auth, team_id, req.user_ids)


@router.delete("", status_code=204)
def remove_team_members(
    team_id: uuid.UUID,
    req: RemoveTeamMembersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀에서 멤버 배치 제거."""
    team_commands.remove_members(db, auth, team_id, req.user_ids)
