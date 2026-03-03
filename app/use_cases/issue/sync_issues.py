"""변경 요청에 이슈 연결 동기화."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncIssuesResponse


@transactional()
def sync_issues(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    issue_ids: list[uuid.UUID],
) -> SyncIssuesResponse:
    """CR-Issue 연결 동기화 — diff 기반으로 추가/제거."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    # Issue 존재 검증
    for iid in issue_ids:
        issue_service.get_or_raise(db, iid)
    added, removed = issue_service.sync_issues(db, cr, issue_ids)
    return SyncIssuesResponse(added_count=len(added), removed_count=len(removed))
