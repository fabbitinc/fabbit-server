"""이슈에서 라벨 배치 해제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service


@transactional()
def unlink_labels(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    label_ids: list[uuid.UUID],
) -> None:
    """이슈에서 라벨 배치 해제."""
    issue = issue_service.get_or_raise(db, issue_id)
    issue_service.unlink_labels(db, issue, label_ids)
