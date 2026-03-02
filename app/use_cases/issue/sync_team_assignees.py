"""이슈 팀 담당자 동기화."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncTeamAssigneesResponse


@transactional()
def sync_team_assignees(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    team_ids: list[uuid.UUID],
) -> SyncTeamAssigneesResponse:
    """이슈 팀 담당자 동기화 — diff 기반으로 추가/제거."""
    issue = issue_service.get_or_raise(db, issue_id)
    added, removed = issue_service.sync_team_assignees(db, issue, team_ids)
    return SyncTeamAssigneesResponse(added_count=len(added), removed_count=len(removed))
