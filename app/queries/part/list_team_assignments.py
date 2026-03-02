"""Part 담당팀 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part import repository as part_repo
from app.modules.part.schemas import (
    PartTeamAssignmentListResponse,
    PartTeamAssignmentSummary,
)
from app.modules.team.models import Team


@transactional(read_only=True)
def list_team_assignments(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
) -> PartTeamAssignmentListResponse:
    """Part 담당팀 목록 조회 — Team 정보 조인."""
    part = part_repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message="Part를 찾을 수 없습니다", code="NOT_FOUND")

    assignments = part_repo.list_team_assignments(db, part_id)
    if not assignments:
        return PartTeamAssignmentListResponse(items=[])

    # Team 정보 배치 조회
    team_ids = list({a.team_id for a in assignments})
    teams = db.query(Team).filter(Team.id.in_(team_ids)).all()
    team_map = {t.id: t for t in teams}

    items = [
        PartTeamAssignmentSummary(
            team_id=a.team_id,
            team_name=team_map[a.team_id].name if a.team_id in team_map else "",
            discipline=a.discipline,
        )
        for a in assignments
    ]
    return PartTeamAssignmentListResponse(items=items)
