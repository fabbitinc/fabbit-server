"""이슈에 부품 배치 연결 -- 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import LinkPartsResponse
from app.modules.part import service as part_service


@transactional()
def link_parts(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> LinkPartsResponse:
    """이슈에 부품 배치 연결."""
    issue = issue_service.get_or_raise(db, issue_id)
    # Part 존재 검증
    for pid in part_ids:
        part_service.get_or_raise(db, pid)
    linked_count = issue_service.link_parts(db, issue, part_ids)
    return LinkPartsResponse(linked_count=linked_count)
