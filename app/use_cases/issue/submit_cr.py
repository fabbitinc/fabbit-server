"""변경 요청 제출 (DRAFT → SUBMITTED)."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def submit_cr(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> ChangeRequestResponse:
    """CR을 검토 상태(SUBMITTED)로 제출한다."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.submit_cr(db, cr)
    return mapper.to_change_request_response(cr)
