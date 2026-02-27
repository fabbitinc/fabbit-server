"""변경 요청 검토 상태 전환."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def open_cr_for_review(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> ChangeRequestResponse:
    """CR을 검토 상태(OPEN)로 전환한다."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.open_cr_for_review(db, cr)
    return mapper.to_change_request_response(cr)
