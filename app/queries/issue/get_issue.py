"""이슈 상세 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.issue import mapper
from app.modules.issue import repository as repo
from app.modules.issue.schemas import IssueResponse
from app.queries.issue._enrichment import load_enrichments


@transactional(read_only=True)
def get_issue(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> IssueResponse:
    """Issue 상세 조회."""
    issue = repo.get_by_id(db, issue_id)
    if not issue:
        raise AppError(
            message=f"Issue '{issue_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    enrichments = load_enrichments(db, [issue])
    e = enrichments[issue.id]

    return mapper.to_issue_response(
        issue,
        created_by_name=e.created_by_name,
        labels=e.labels,
        assignees=e.assignees,
        parts=e.parts,
        files=e.files,
        comments_count=e.comments_count,
    )
