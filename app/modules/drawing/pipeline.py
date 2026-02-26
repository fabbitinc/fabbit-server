"""도면 변환 백그라운드 파이프라인.

HTTP 요청 외부에서 자체 세션 생성 + 커밋으로 동작하며,
일반 서비스 레이어 규칙의 예외로 처리한다.
cross-domain File 모델 접근은 파이프라인 특성상 필요하다.
"""

import uuid

from loguru import logger

from app.core.database import create_tenant_session, generate_uuid7
from app.core.uow import UnitOfWork
from app.infrastructure.drawing_converter import convert_drawing
from app.infrastructure.s3_client import s3_client
from app.modules.drawing import repository as repo
from app.modules.drawing.constants import ConversionStatus
from app.modules.file.models import File

_s3 = s3_client


def run_conversion(
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
    db,
    drawing_id: uuid.UUID,
    file_id: uuid.UUID,
    status: ConversionStatus,
    pdf_key: str | None,
    pdf_content_type: str | None,
    pdf_size: int | None,
    thumbnail_key: str | None,
    thumbnail_content_type: str | None,
    thumbnail_size: int | None,
    error: str | None,
) -> None:
    """단일 변환 결과를 Drawing에 반영."""
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
                pdf_file = _create_file_record(
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
                thumb_file = _create_file_record(
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


def _create_file_record(
    db,
    file_id: uuid.UUID,
    original_name: str,
    file_key: str,
    content_type: str,
    file_size: int,
    owner_type: str | None,
    owner_id: uuid.UUID | None,
) -> File:
    """File 레코드 생성 — pipeline 내 cross-domain 접근."""
    file = File(
        id=file_id,
        original_name=original_name,
        file_key=file_key,
        content_type=content_type,
        file_size=file_size,
        owner_type=owner_type,
        owner_id=owner_id,
    )
    db.add(file)
    return file
