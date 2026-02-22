"""업로드 도메인 서비스 레이어."""

import uuid
from datetime import datetime, timedelta, timezone

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.config import settings
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.drawing_converter_client import DrawingConverterClient
from app.infrastructure.s3_client import S3Client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.upload import repository as repo
from app.modules.upload.constants import ConversionStatus, UploadStatus
from app.modules.upload.models import Upload
from app.modules.upload.schemas import (
    BatchCompleteFailure,
    BatchCompleteRequest,
    BatchCompleteResponse,
    BatchCreateUploadRequest,
    BatchCreateUploadResponse,
    ConversionResultRequest,
    CreateUploadRequest,
    CreateUploadResponse,
    UploadCompleteResponse,
)

_s3 = S3Client()
_converter = DrawingConverterClient()

# DWG 확장자 (대소문자 무시)
_DWG_EXTENSIONS = {".dwg"}


@transactional
def create_upload(
    db: Session,
    auth: AuthContext,
    req: CreateUploadRequest,
) -> CreateUploadResponse:
    upload_id = generate_uuid7()
    file_key = f"tenants/{auth.org_id}/raw_data/{upload_id}/{req.original_name}"

    repo.create_upload_record(
        db=db,
        upload_id=upload_id,
        original_name=req.original_name,
        file_key=file_key,
        content_type=req.content_type,
        file_size=req.file_size,
        owner_type=req.owner_type,
        owner_id=req.owner_id,
    )

    presigned = _s3.generate_upload_presigned_url(
        file_key=file_key,
        content_type=req.content_type,
        content_length=req.file_size,
    )
    logger.info(
        "업로드 URL 발급: upload_id={upload_id} file_key={file_key}",
        upload_id=upload_id,
        file_key=file_key,
    )
    return CreateUploadResponse(
        upload_id=upload_id,
        upload_url=presigned["upload_url"],
        file_key=file_key,
    )


@transactional
def batch_create_uploads(
    db: Session,
    auth: AuthContext,
    req: BatchCreateUploadRequest,
) -> BatchCreateUploadResponse:
    results: list[CreateUploadResponse] = []

    for item in req.items:
        upload_id = generate_uuid7()
        file_key = f"tenants/{auth.org_id}/raw_data/{upload_id}/{item.original_name}"

        repo.create_upload_record(
            db=db,
            upload_id=upload_id,
            original_name=item.original_name,
            file_key=file_key,
            content_type=item.content_type,
            file_size=item.file_size,
            owner_type=item.owner_type,
            owner_id=item.owner_id,
        )

        presigned = _s3.generate_upload_presigned_url(
            file_key=file_key,
            content_type=item.content_type,
            content_length=item.file_size,
        )
        results.append(
            CreateUploadResponse(
                upload_id=upload_id,
                upload_url=presigned["upload_url"],
                file_key=file_key,
            )
        )

    logger.info("배치 업로드 URL 발급: {count}건", count=len(results))
    return BatchCreateUploadResponse(items=results)


@transactional
def batch_complete_uploads(
    db: Session,
    req: BatchCompleteRequest,
) -> BatchCompleteResponse:
    completed: list[UploadCompleteResponse] = []
    failed: list[BatchCompleteFailure] = []

    uploads = repo.get_uploads_by_ids(db, req.upload_ids)
    upload_map = {u.id: u for u in uploads}

    for upload_id in req.upload_ids:
        upload = upload_map.get(upload_id)
        if upload is None:
            failed.append(
                BatchCompleteFailure(
                    upload_id=upload_id,
                    reason="업로드를 찾을 수 없습니다",
                )
            )
            continue

        if upload.status == UploadStatus.UPLOADED:
            failed.append(
                BatchCompleteFailure(
                    upload_id=upload_id,
                    reason="이미 완료된 업로드입니다",
                )
            )
            continue

        obj_meta = _s3.head_object(upload.file_key)
        if obj_meta is None:
            failed.append(
                BatchCompleteFailure(
                    upload_id=upload_id,
                    reason="S3에 파일이 존재하지 않습니다",
                )
            )
            continue

        upload.mark_uploaded()
        completed.append(_to_upload_complete_response(upload))

    logger.info(
        "배치 업로드 완료: 성공={ok}건 실패={fail}건",
        ok=len(completed),
        fail=len(failed),
    )
    return BatchCompleteResponse(items=completed, failed=failed)


@transactional
def complete_upload(
    db: Session,
    upload_id: uuid.UUID,
    auth: AuthContext,
) -> UploadCompleteResponse:
    upload = repo.get_upload_by_id(db, upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    if upload.status == UploadStatus.UPLOADED:
        raise AppError(message="이미 완료된 업로드입니다", code="CONFLICT")

    obj_meta = _s3.head_object(upload.file_key)
    if obj_meta is None:
        raise AppError(
            message="S3에 파일이 존재하지 않습니다. 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    upload.mark_uploaded()

    # DWG 파일이면 변환 요청 트리거
    if _is_dwg_file(upload.original_name) and _converter.enabled:
        upload.request_conversion()
        tenant_schema = org_id_to_schema(auth.org_id)
        callback_url = (
            f"{settings.base_api_url}/api/v1/internal/webhooks/drawing-converter"
        )
        try:
            _converter.request_conversion(
                upload_id=upload.id,
                tenant_schema=tenant_schema,
                file_key=upload.file_key,
                callback_url=callback_url,
            )
        except Exception:
            logger.warning(
                "변환 요청 실패 — 업로드는 정상 처리: upload_id={upload_id}",
                upload_id=upload.id,
            )
            upload.fail_conversion()

    logger.info(
        "업로드 완료: upload_id={upload_id} size={size}",
        upload_id=upload.id,
        size=obj_meta["content_length"],
    )
    return _to_upload_complete_response(upload)


def handle_conversion_result(req: ConversionResultRequest) -> None:
    """Webhook으로 수신한 변환 결과를 Upload에 반영.

    webhook은 테넌트 인증 없이 호출되므로 create_tenant_session을 사용합니다.
    """
    db = create_tenant_session(req.tenant_schema)
    try:
        upload = repo.get_upload_by_id(db, req.upload_id)
        if upload is None:
            logger.warning(
                "변환 결과 수신 — 업로드 없음: upload_id={upload_id}",
                upload_id=req.upload_id,
            )
            return

        if req.status == ConversionStatus.COMPLETED:
            upload.complete_conversion(req.pdf_key, req.thumbnail_key)
        else:
            upload.fail_conversion()
            logger.warning(
                "변환 실패: upload_id={upload_id} error={error}",
                upload_id=req.upload_id,
                error=req.error,
            )

        db.commit()
        logger.info(
            "변환 결과 반영: upload_id={upload_id} status={status}",
            upload_id=req.upload_id,
            status=req.status,
        )
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


@transactional
def soft_delete_upload(db: Session, upload_id: uuid.UUID) -> None:
    """업로드를 소프트 삭제 처리. S3 파일은 cleanup 배치에서 제거."""
    upload = repo.get_upload_by_id(db, upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    upload.mark_deleted()


@transactional
def soft_delete_uploads(db: Session, upload_ids: list[uuid.UUID]) -> int:
    """여러 업로드를 소프트 삭제 처리. 삭제된 건수 반환."""
    uploads = repo.get_uploads_by_ids(db, upload_ids)
    count = 0
    for upload in uploads:
        if upload.status != UploadStatus.DELETED:
            upload.mark_deleted()
            count += 1
    return count


def cleanup_stale_uploads(
    tenant_schema: str,
    days: int = 1,
    batch_size: int = 100,
) -> int:
    """PENDING 상태로 오래된 업로드 정리. S3 삭제 + EXPIRED 처리."""
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)
    db = create_tenant_session(tenant_schema)
    try:
        count = _cleanup_batch(db, UploadStatus.PENDING, cutoff, batch_size, expire=True)
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()
    if count:
        logger.info(
            "stale 정리: schema={schema} count={count}",
            schema=tenant_schema,
            count=count,
        )
    return count


def cleanup_orphan_files(s3_prefix: str, tenant_schema: str) -> int:
    """S3에 존재하지만 DB에 레코드가 없는 고아 파일 삭제."""
    db = create_tenant_session(tenant_schema)
    try:
        known_keys = repo.get_all_file_keys(db)
    finally:
        db.close()

    s3_keys = _s3.list_keys(s3_prefix)

    orphan_keys = [k for k in s3_keys if k not in known_keys]
    for key in orphan_keys:
        try:
            _s3.delete_object(key)
        except Exception:
            logger.warning("S3 orphan 삭제 실패: key={key}", key=key)

    if orphan_keys:
        logger.info(
            "orphan 파일 정리: prefix={prefix} count={count}",
            prefix=s3_prefix,
            count=len(orphan_keys),
        )
    return len(orphan_keys)


def cleanup_deleted_uploads(
    tenant_schema: str,
    days: int = 7,
    batch_size: int = 100,
) -> int:
    """DELETED 상태로 보존 기간 만료된 업로드 물리 삭제. S3 삭제 + 레코드 삭제."""
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)
    db = create_tenant_session(tenant_schema)
    try:
        count = _cleanup_batch(db, UploadStatus.DELETED, cutoff, batch_size, expire=False)
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()
    if count:
        logger.info(
            "deleted 정리: schema={schema} count={count}",
            schema=tenant_schema,
            count=count,
        )
    return count


def _cleanup_batch(
    db: Session,
    status: str,
    cutoff: datetime,
    batch_size: int,
    expire: bool,
) -> int:
    """배치 단위로 S3 삭제 + DB 상태 변경/레코드 삭제."""
    count = 0
    cursor = None
    while True:
        uploads = repo.get_stale_uploads(db, status, cutoff, batch_size, cursor)
        if not uploads:
            break

        for upload in uploads:
            _delete_s3_files(upload)
            if expire:
                upload.mark_expired()
            else:
                db.delete(upload)
            count += 1

        cursor = uploads[-1].id

    return count


def _delete_s3_files(upload: Upload) -> None:
    """업로드에 연결된 모든 S3 파일 삭제 (원본 + pdf + 썸네일)."""
    for key in [upload.file_key, upload.pdf_key, upload.thumbnail_key]:
        if key:
            try:
                _s3.delete_object(key)
            except Exception:
                logger.warning(
                    "S3 파일 삭제 실패: key={key} upload_id={upload_id}",
                    key=key,
                    upload_id=upload.id,
                )


def _is_dwg_file(filename: str) -> bool:
    """파일명이 DWG 확장자인지 판별."""
    import os

    _, ext = os.path.splitext(filename)
    return ext.lower() in _DWG_EXTENSIONS


def _to_upload_complete_response(upload: Upload) -> UploadCompleteResponse:
    return UploadCompleteResponse(
        upload_id=upload.id,
        status=upload.status,
        original_name=upload.original_name,
        file_key=upload.file_key,
        file_size=upload.file_size,
        content_type=upload.content_type,
        conversion_status=upload.conversion_status,
        created_at=upload.created_at,
    )
