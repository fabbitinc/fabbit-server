"""이슈 재개."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import IssueResponse


@transactional()
def reopen_issue(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> IssueResponse:
    """닫힌 이슈를 다시 연다."""
    issue = issue_service.get_or_raise(db, issue_id)
    issue_service.reopen_issue(db, issue)
    return mapper.to_issue_response(issue)
