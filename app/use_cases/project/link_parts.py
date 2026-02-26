"""Project에 Part 배치 연결 — 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import service as part_service
from app.modules.project import service as project_service
from app.modules.project.schemas import LinkPartsResponse


@transactional()
def link_parts(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> LinkPartsResponse:
    """Project에 Part 배치 연결."""
    project_service.get_or_raise(db, project_id)
    # Part 존재 검증
    for pid in part_ids:
        part_service.get_or_raise(db, pid)
    linked_count = project_service.link_parts(db, project_id, part_ids)
    return LinkPartsResponse(linked_count=linked_count)
