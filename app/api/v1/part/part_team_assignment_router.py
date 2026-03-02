"""Part 담당팀 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.part.schemas import (
    ManageAssignmentsResponse,
    ManageTeamAssignmentsRequest,
    PartTeamAssignmentListResponse,
)
from app.queries import part as part_queries
from app.use_cases import part as part_commands

router = APIRouter(
    prefix="/api/v1/parts/{part_id}/assigned-teams",
    tags=["part-team-assignments"],
)


@router.get("", response_model=PartTeamAssignmentListResponse)
def list_team_assignments(
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당팀 목록 조회.

    해당 Part에 배정된 담당팀(Team) 목록을 discipline 포함하여 반환합니다.
    """
    return part_queries.list_team_assignments(db, auth, part_id)


@router.post("", response_model=ManageAssignmentsResponse, status_code=201)
def add_team_assignments(
    part_id: uuid.UUID,
    req: ManageTeamAssignmentsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당팀 배치 추가.

    동일 Part에 같은 Team + 다른 discipline 조합은 별도 배정으로 처리됩니다.
    이미 존재하는 (team_id, discipline) 조합은 무시됩니다 (멱등성).
    """
    assignments = [
        {"team_id": a.team_id, "discipline": a.discipline}
        for a in req.assignments
    ]
    return part_commands.add_team_assignments(db, auth, part_id, assignments)


@router.delete("", status_code=204)
def remove_team_assignments(
    part_id: uuid.UUID,
    req: ManageTeamAssignmentsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당팀 배치 제거.

    지정된 (team_id, discipline) 조합을 일괄 삭제합니다.
    """
    assignments = [
        {"team_id": a.team_id, "discipline": a.discipline}
        for a in req.assignments
    ]
    part_commands.remove_team_assignments(db, auth, part_id, assignments)
