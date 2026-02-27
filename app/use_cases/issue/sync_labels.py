"""이슈 라벨 동기화 -- 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncLabelsResponse
from app.modules.label import service as label_service


@transactional()
def sync_labels(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    label_ids: list[uuid.UUID],
) -> SyncLabelsResponse:
    """이슈 라벨 동기화 — diff 기반으로 추가/제거."""
    issue = issue_service.get_or_raise(db, issue_id)
    # Label 존재 검증
    for lid in label_ids:
        label_service.get_or_raise(db, lid)
    added, removed = issue_service.sync_labels(db, issue, label_ids)
    return SyncLabelsResponse(added_count=len(added), removed_count=len(removed))
