"""변경 요청 수정 — 제목/본문 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def update_change_request(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    title: str | None = None,
    body: str | None = None,
) -> ChangeRequestResponse:
    """변경 요청 제목/본문 수정."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.update_issue(db, cr, title=title, body=body)
    return mapper.to_change_request_response(cr)
