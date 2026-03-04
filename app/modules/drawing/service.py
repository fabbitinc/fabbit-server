"""도면 도메인 서비스 레이어."""

import os
import uuid
from typing import cast

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.background_worker import guarded
from app.core.exceptions import AppError
from app.modules.organization.provisioning import org_id_to_schema
from app.modules.drawing import repository as repo
from app.modules.drawing.constants import ALLOWED_DRAWING_EXTENSIONS, ConversionStatus
from app.modules.drawing.models import Drawing
from app.modules.drawing.pipeline import run_conversion
from app.modules.drawing.schemas import RegisterDrawingResponse
from app.modules.file.models import File

# ── Drawing 등록/삭제 ──


def create_drawing(
    db: Session,
    file: File,
    auth: AuthContext,
    add_background_task,
) -> Drawing:
    """Drawing 레코드 생성 + 변환 백그라운드 작업 등록."""
    _validate_drawing_file(file)
    drawing = Drawing.create_pending(
        original_file_id=file.id,
        original_file_key=file.file_key,
        original_name=file.original_name,
    )
    db.add(drawing)
    db.flush()
    drawing.attach_original_file(file, org_id=auth.org_id)
    add_background_task(
        guarded(run_conversion),
        file_key=file.file_key,
        drawing_id=drawing.id,
        file_id=file.id,
        tenant_schema=org_id_to_schema(auth.org_id),
        org_id=auth.org_id,
    )
    return drawing


def delete_drawing(db: Session, drawing_id: uuid.UUID) -> None:
    """Drawing 삭제 — 연결 파일 정리는 FileHandler가 이벤트로 처리."""
    drawing = repo.get_drawing_by_id(db, drawing_id)
    if drawing is None:
        raise AppError(message="도면을 찾을 수 없습니다", code="NOT_FOUND")
    drawing.soft_delete()


def to_register_response(drawing: Drawing) -> RegisterDrawingResponse:
    """Drawing을 등록 응답으로 변환."""
    return RegisterDrawingResponse(
        drawing_id=drawing.id,
        drawing_number=drawing.drawing_number,
        name=drawing.name,
        conversion_status=cast(ConversionStatus | None, drawing.conversion_status),
    )


# ── 내부 함수 ──


def _validate_drawing_file(file: File) -> None:
    """도면 등록 가능한 파일 형식인지 검증."""
    _, ext = os.path.splitext(file.original_name)
    if ext.lower() not in ALLOWED_DRAWING_EXTENSIONS:
        raise AppError(
            message=f"도면으로 등록할 수 없는 파일 형식입니다: {ext}",
            code="UNSUPPORTED_FORMAT",
        )
