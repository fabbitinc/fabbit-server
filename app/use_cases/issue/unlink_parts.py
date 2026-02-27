"""이슈에서 부품 배치 해제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service


@transactional()
def unlink_parts(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> None:
    """이슈에서 부품 배치 해제."""
    issue_service.get_or_raise(db, issue_id)
    issue_service.unlink_parts(db, issue_id, part_ids)
