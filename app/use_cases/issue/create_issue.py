"""이슈 생성."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import IssueResponse


@transactional()
def create_issue(
    db: Session,
    auth: AuthContext,
    title: str,
    body: str | None = None,
) -> IssueResponse:
    """이슈 생성."""
    issue = issue_service.create_issue(db, title, body)
    return mapper.to_issue_response(issue)
