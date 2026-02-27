"""변경 요청에 이슈 배치 연결 — 이슈 존재 검증 포함."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import LinkIssuesResponse


@transactional()
def link_issues(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    issue_ids: list[uuid.UUID],
) -> LinkIssuesResponse:
    """CR에 이슈 배치 연결."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    # Issue 존재 검증
    for iid in issue_ids:
        issue_service.get_or_raise(db, iid)
    linked_count = issue_service.link_issues(db, cr, issue_ids)
    return LinkIssuesResponse(linked_count=linked_count)
