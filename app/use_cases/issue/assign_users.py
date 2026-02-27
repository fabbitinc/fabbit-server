"""이슈 담당자 배치 할당."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import AssignUsersResponse


@transactional()
def assign_users(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> AssignUsersResponse:
    """이슈 담당자 배치 할당."""
    issue_service.get_or_raise(db, issue_id)
    assigned_count = issue_service.assign_users(db, issue_id, user_ids)
    return AssignUsersResponse(assigned_count=assigned_count)
