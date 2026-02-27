"""이슈에 라벨 배치 연결 -- 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import LinkLabelsResponse
from app.modules.label import service as label_service


@transactional()
def link_labels(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    label_ids: list[uuid.UUID],
) -> LinkLabelsResponse:
    """이슈에 라벨 배치 연결."""
    issue = issue_service.get_or_raise(db, issue_id)
    # Label 존재 검증
    for lid in label_ids:
        label_service.get_or_raise(db, lid)
    linked_count = issue_service.link_labels(db, issue, label_ids)
    return LinkLabelsResponse(linked_count=linked_count)
