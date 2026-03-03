"""이슈에 변경 요청 연결 동기화 (역방향)."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncChangesResponse


@transactional()
def sync_changes(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    cr_ids: list[uuid.UUID],
) -> SyncChangesResponse:
    """Issue-CR 연결 동기화 — diff 기반으로 추가/제거."""
    issue = issue_service.get_or_raise(db, issue_id)
    # CR 존재 검증
    for cid in cr_ids:
        issue_service.get_cr_or_raise(db, cid)
    added, removed = issue_service.sync_changes(db, issue, cr_ids)
    return SyncChangesResponse(added_count=len(added), removed_count=len(removed))
