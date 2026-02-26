"""Part에 연결된 도면 삭제 — 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.drawing import service as drawing_service
from app.modules.part import service as part_service


@transactional()
def delete_drawing(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
) -> None:
    """Part에 연결된 도면 삭제."""
    drawing_id = part_service.unassign_drawing(db, part_id)
    drawing_service.delete_drawing(db, drawing_id)
