"""이슈 부품 동기화 -- 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SyncPartsResponse
from app.modules.project import service as project_service


@transactional()
def sync_parts(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> SyncPartsResponse:
    """이슈 부품 동기화 — diff 기반으로 추가/제거."""
    issue = issue_service.get_or_raise(db, issue_id)
    # 프로젝트에 연결된 부품인지 검증
    if part_ids:
        project_service.validate_parts_in_project(db, project_id, part_ids)
    added, removed = issue_service.sync_parts(db, issue, part_ids)
    return SyncPartsResponse(added_count=len(added), removed_count=len(removed))
