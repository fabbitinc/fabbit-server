"""도면 도메인 서비스 레이어."""

import os
import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.background_worker import guarded
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.uow import UnitOfWork
from app.infrastructure.drawing_converter import convert_drawing
from app.infrastructure.s3_client import s3_client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.drawing import repository as repo
from app.modules.drawing.constants import ALLOWED_DRAWING_EXTENSIONS, ConversionStatus
from app.modules.drawing.models import Drawing
from app.modules.drawing.schemas import RegisterDrawingResponse
from app.modules.file import repository as file_repo
from app.modules.file.models import File

_s3 = s3_client

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
    add_background_task(
        guarded(_run_conversion),
        file_key=file.file_key,
        drawing_id=drawing.id,
        file_id=file.id,
        tenant_schema=org_id_to_schema(auth.org_id),
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
        conversion_status=drawing.conversion_status,
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


def _run_conversion(
    file_key: str,
    drawing_id: uuid.UUID,
    file_id: uuid.UUID,
    tenant_schema: str,
) -> None:
    """BackgroundTask — 도면 변환 실행 후 Drawing에 결과 반영."""
    status = ConversionStatus.FAILED
    pdf_key = None
    pdf_content_type = None
    pdf_size = None
    thumbnail_key = None
    thumbnail_content_type = None
    thumbnail_size = None
    error = None

    try:
        result = convert_drawing(file_key, _s3)
        status = ConversionStatus.COMPLETED
        pdf_key = result.pdf_key
        pdf_content_type = result.pdf_content_type
        pdf_size = result.pdf_size
        thumbnail_key = result.thumbnail_key
        thumbnail_content_type = result.thumbnail_content_type
        thumbnail_size = result.thumbnail_size
    except Exception as exc:
        error = str(exc)
        logger.error(
            "도면 변환 실패: drawing_id={drawing_id} file_key={file_key} error={error}",
            drawing_id=drawing_id,
            file_key=file_key,
            error=error,
        )

    db = create_tenant_session(tenant_schema)
    try:
        _apply_conversion_result(
            db,
            drawing_id=drawing_id,
            file_id=file_id,
            status=status,
            pdf_key=pdf_key,
            pdf_content_type=pdf_content_type,
            pdf_size=pdf_size,
            thumbnail_key=thumbnail_key,
            thumbnail_content_type=thumbnail_content_type,
            thumbnail_size=thumbnail_size,
            error=error,
        )
        UnitOfWork(db).commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def _apply_conversion_result(
    db: Session,
    drawing_id: uuid.UUID,
    file_id: uuid.UUID,
    status: str,
    pdf_key: str | None,
    pdf_content_type: str | None,
    pdf_size: int | None,
    thumbnail_key: str | None,
    thumbnail_content_type: str | None,
    thumbnail_size: int | None,
    error: str | None,
) -> None:
    """단일 변환 결과를 Drawing에 반영 (공통 로직)."""
    drawing = repo.get_drawing_by_id(db, drawing_id)
    if drawing is None:
        logger.warning(
            "변환 결과 수신 — Drawing 없음: drawing_id={drawing_id} file_id={file_id}",
            drawing_id=drawing_id,
            file_id=file_id,
        )
        return

    if status == ConversionStatus.COMPLETED:
        # PDF File 레코드 — 원본과 동일하면 재사용, 다르면 새로 생성
        pdf_file_id = None
        if pdf_key:
            if pdf_key == drawing.original_file_key:
                pdf_file_id = drawing.original_file_id
            else:
                pdf_file = file_repo.create_file_record(
                    db,
                    file_id=generate_uuid7(),
                    original_name=f"{drawing.name}.pdf",
                    file_key=pdf_key,
                    content_type=pdf_content_type or "application/pdf",
                    file_size=pdf_size or 0,
                    owner_type="drawing",
                    owner_id=drawing.id,
                )
                pdf_file.mark_uploaded()
                pdf_file_id = pdf_file.id

        # Thumbnail File 레코드 — 원본과 동일하면 재사용, 다르면 새로 생성
        thumbnail_file_id = None
        if thumbnail_key:
            if thumbnail_key == drawing.original_file_key:
                thumbnail_file_id = drawing.original_file_id
            else:
                thumb_file = file_repo.create_file_record(
                    db,
                    file_id=generate_uuid7(),
                    original_name=f"{drawing.name}_thumb.webp",
                    file_key=thumbnail_key,
                    content_type=thumbnail_content_type or "image/webp",
                    file_size=thumbnail_size or 0,
                    owner_type="drawing",
                    owner_id=drawing.id,
                )
                thumb_file.mark_uploaded()
                thumbnail_file_id = thumb_file.id

        drawing.complete_conversion(
            pdf_file_id=pdf_file_id,
            pdf_key=pdf_key,
            thumbnail_file_id=thumbnail_file_id,
            thumbnail_key=thumbnail_key,
        )
    else:
        drawing.fail_conversion()
        logger.warning(
            "변환 실패: drawing_id={drawing_id} error={error}",
            drawing_id=drawing_id,
            error=error,
        )

    logger.info(
        "변환 결과 반영: drawing_id={drawing_id} status={status}",
        drawing_id=drawing_id,
        status=status,
    )
