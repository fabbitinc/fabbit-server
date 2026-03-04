"""Part에 도면 추가 — 크로스 도메인 오케스트레이션."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.drawing import service as drawing_service
from app.modules.drawing.schemas import RegisterDrawingResponse
from app.modules.file import service as file_service
from app.modules.part import service as part_service


@transactional()
def add_drawing(
    db: Session,
    auth: AuthContext,
    file_id: uuid.UUID,
    part_id: uuid.UUID,
    add_background_task,
) -> RegisterDrawingResponse:
    """Part에 도면 추가.

    기존 도면이 있으면 연결 해제 후 새 도면을 생성한다.
    기존 Drawing은 고아 상태로 기록 유지된다.
    """
    file = file_service.get_uploaded_or_raise(db, file_id)
    drawing = drawing_service.create_drawing(db, file, auth, add_background_task)
    part_service.assign_drawing(db, part_id, drawing.id)
    return drawing_service.to_register_response(drawing)
