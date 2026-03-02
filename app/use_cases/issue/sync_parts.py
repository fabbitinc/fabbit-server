"""이슈 부품 동기화."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncPartsResponse


@transactional()
def sync_parts(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> SyncPartsResponse:
    """이슈 부품 동기화 — diff 기반으로 추가/제거."""
    issue = issue_service.get_or_raise(db, issue_id)
    added, removed = issue_service.sync_parts(db, issue, part_ids)
    return SyncPartsResponse(added_count=len(added), removed_count=len(removed))
