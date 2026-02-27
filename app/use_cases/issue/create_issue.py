"""이슈 생성 — 프로젝트 존재 검증 후 이슈 생성."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import IssueResponse
from app.modules.project import service as project_service


@transactional()
def create_issue(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    title: str,
    body: str | None = None,
) -> IssueResponse:
    """이슈 생성."""
    project_service.get_or_raise(db, project_id)
    issue = issue_service.create_issue(db, project_id, title, body)
    return mapper.to_issue_response(issue)
