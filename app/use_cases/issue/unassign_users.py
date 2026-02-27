"""이슈 담당자 배치 해제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service


@transactional()
def unassign_users(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> None:
    """이슈 담당자 배치 해제."""
    issue = issue_service.get_or_raise(db, issue_id)
    issue_service.unassign_users(db, issue, user_ids)
