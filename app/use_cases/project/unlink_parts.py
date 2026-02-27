"""Project에서 Part 배치 해제 — 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import service as project_service


@transactional()
def unlink_parts(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    part_ids: list[uuid.UUID],
) -> None:
    """Project에서 Part 배치 해제."""
    project = project_service.get_or_raise(db, project_id)
    project_service.unlink_parts(db, project, part_ids)
