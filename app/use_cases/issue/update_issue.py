"""이슈 수정 — 제목/본문 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import IssueResponse


@transactional()
def update_issue(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    title: str | None = None,
    body: str | None = None,
) -> IssueResponse:
    """이슈 제목/본문 수정."""
    issue = issue_service.get_or_raise(db, issue_id)
    issue = issue_service.update_issue(db, issue, title=title, body=body)
    return mapper.to_issue_response(issue)
