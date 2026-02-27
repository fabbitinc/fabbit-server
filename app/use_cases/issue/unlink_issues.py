"""변경 요청에서 이슈 배치 해제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service


@transactional()
def unlink_issues(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    issue_ids: list[uuid.UUID],
) -> None:
    """CR에서 이슈 배치 해제."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.unlink_issues(db, cr, issue_ids)
