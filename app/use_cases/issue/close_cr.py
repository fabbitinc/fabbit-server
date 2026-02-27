"""변경 요청 닫기."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def close_cr(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> ChangeRequestResponse:
    """변경 요청을 닫는다."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.close_cr(db, cr)
    return mapper.to_change_request_response(cr)
