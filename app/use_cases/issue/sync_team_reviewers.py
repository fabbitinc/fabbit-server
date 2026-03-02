"""변경 요청 팀 검토자 동기화."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncTeamReviewersResponse


@transactional()
def sync_team_reviewers(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    team_ids: list[uuid.UUID],
) -> SyncTeamReviewersResponse:
    """CR 팀 검토자 동기화 — diff 기반으로 추가/제거."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    added, removed = issue_service.sync_team_reviewers(db, cr, team_ids)
    return SyncTeamReviewersResponse(added_count=len(added), removed_count=len(removed))
