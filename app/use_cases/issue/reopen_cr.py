"""변경 요청 다시 열기 (CLOSED → OPEN)."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def reopen_cr(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> ChangeRequestResponse:
    """닫힌 변경 요청을 검토 상태(OPEN)로 다시 연다."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.reopen_cr(db, cr)
    return mapper.to_change_request_response(cr)
