"""이슈 부품 동기화 -- 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncPartsResponse
from app.modules.part import service as part_service


@transactional()
def sync_parts(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> SyncPartsResponse:
    """이슈 부품 동기화 — diff 기반으로 추가/제거."""
    issue = issue_service.get_or_raise(db, issue_id)
    # 새로 추가될 Part 존재 검증
    for pid in part_ids:
        part_service.get_or_raise(db, pid)
    added, removed = issue_service.sync_parts(db, issue, part_ids)
    return SyncPartsResponse(added_count=len(added), removed_count=len(removed))
