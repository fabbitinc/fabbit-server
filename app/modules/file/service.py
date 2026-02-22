"""파일 도메인 서비스 레이어."""

import os
import uuid
from datetime import datetime, timedelta, timezone

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.drawing_converter_client import DrawingConverterClient
from app.infrastructure.s3_client import S3Client
from app.modules.file import repository as repo
from app.modules.file.constants import FileStatus
from app.modules.file.models import File
from app.modules.file.schemas import (
    BatchCompleteFailure,
    BatchCompleteRequest,
    BatchCompleteResponse,
    BatchCreateFileRequest,
    BatchCreateFileResponse,
    CreateFileRequest,
    CreateFileResponse,
    FileCompleteResponse,
)

_s3 = S3Client()
_converter = DrawingConverterClient()

# DWG 확장자 (대소문자 무시)
_DWG_EXTENSIONS = {".dwg"}


@transactional
def create_file(
    db: Session,
    auth: AuthContext,
    req: CreateFileRequest,
) -> CreateFileResponse:
    file_id = generate_uuid7()
    file_key = f"tenants/{auth.org_id}/raw_data/{file_id}/{req.original_name}"

    repo.create_file_record(
        db=db,
        file_id=file_id,
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
        "업로드 URL 발급: file_id={file_id} file_key={file_key}",
        file_id=file_id,
        file_key=file_key,
    )
    return CreateFileResponse(
        file_id=file_id,
        upload_url=presigned["upload_url"],
        file_key=file_key,
    )


@transactional
def batch_create_files(
    db: Session,
    auth: AuthContext,
    req: BatchCreateFileRequest,
) -> BatchCreateFileResponse:
    results: list[CreateFileResponse] = []

    for item in req.items:
        file_id = generate_uuid7()
        file_key = f"tenants/{auth.org_id}/raw_data/{file_id}/{item.original_name}"

        repo.create_file_record(
            db=db,
            file_id=file_id,
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
            CreateFileResponse(
                file_id=file_id,
                upload_url=presigned["upload_url"],
                file_key=file_key,
            )
        )

    logger.info("배치 업로드 URL 발급: {count}건", count=len(results))
    return BatchCreateFileResponse(items=results)


@transactional
def batch_complete_files(
    db: Session,
    req: BatchCompleteRequest,
) -> BatchCompleteResponse:
    completed: list[FileCompleteResponse] = []
    failed: list[BatchCompleteFailure] = []

    files = repo.get_files_by_ids(db, req.file_ids)
    file_map = {f.id: f for f in files}

    for file_id in req.file_ids:
        file = file_map.get(file_id)
        if file is None:
            failed.append(
                BatchCompleteFailure(
                    file_id=file_id,
                    reason="파일을 찾을 수 없습니다",
                )
            )
            continue

        if file.status == FileStatus.UPLOADED:
            failed.append(
                BatchCompleteFailure(
                    file_id=file_id,
                    reason="이미 완료된 업로드입니다",
                )
            )
            continue

        obj_meta = _s3.head_object(file.file_key)
        if obj_meta is None:
            failed.append(
                BatchCompleteFailure(
                    file_id=file_id,
                    reason="S3에 파일이 존재하지 않습니다",
                )
            )
            continue

        file.mark_uploaded()
        completed.append(_to_file_complete_response(file))

    logger.info(
        "배치 업로드 완료: 성공={ok}건 실패={fail}건",
        ok=len(completed),
        fail=len(failed),
    )
    return BatchCompleteResponse(items=completed, failed=failed)


@transactional
def complete_file(
    db: Session,
    file_id: uuid.UUID,
    auth: AuthContext,
) -> FileCompleteResponse:
    file = repo.get_file_by_id(db, file_id)
    if file is None:
        raise AppError(message="파일을 찾을 수 없습니다", code="NOT_FOUND")

    if file.status == FileStatus.UPLOADED:
        raise AppError(message="이미 완료된 업로드입니다", code="CONFLICT")

    obj_meta = _s3.head_object(file.file_key)
    if obj_meta is None:
        raise AppError(
            message="S3에 파일이 존재하지 않습니다. 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    file.mark_uploaded()

    # DWG 파일이면 예비 Drawing 생성 + 변환 트리거
    if _is_dwg_file(file.original_name) and _converter.enabled:
        from app.modules.drawing import service as drawing_service

        drawing_service.create_pending_drawing(db, file, auth)

    logger.info(
        "업로드 완료: file_id={file_id} size={size}",
        file_id=file.id,
        size=obj_meta["content_length"],
    )
    return _to_file_complete_response(file)


@transactional
def soft_delete_file(db: Session, file_id: uuid.UUID) -> None:
    """파일을 소프트 삭제 처리. S3 파일은 cleanup 배치에서 제거."""
    file = repo.get_file_by_id(db, file_id)
    if file is None:
        raise AppError(message="파일을 찾을 수 없습니다", code="NOT_FOUND")

    file.mark_deleted()


@transactional
def soft_delete_files(db: Session, file_ids: list[uuid.UUID]) -> int:
    """여러 파일을 소프트 삭제 처리. 삭제된 건수 반환."""
    files = repo.get_files_by_ids(db, file_ids)
    count = 0
    for file in files:
        if file.status != FileStatus.DELETED:
            file.mark_deleted()
            count += 1
    return count


def cleanup_stale_files(
    tenant_schema: str,
    days: int = 1,
    batch_size: int = 100,
) -> int:
    """PENDING 상태로 오래된 파일 정리. S3 삭제 + EXPIRED 처리."""
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)
    db = create_tenant_session(tenant_schema)
    try:
        count = _cleanup_batch(db, FileStatus.PENDING, cutoff, batch_size, expire=True)
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


def cleanup_deleted_files(
    tenant_schema: str,
    days: int = 7,
    batch_size: int = 100,
) -> int:
    """DELETED 상태로 보존 기간 만료된 파일 물리 삭제. S3 삭제 + 레코드 삭제."""
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)
    db = create_tenant_session(tenant_schema)
    try:
        count = _cleanup_batch(db, FileStatus.DELETED, cutoff, batch_size, expire=False)
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
        files = repo.get_stale_files(db, status, cutoff, batch_size, cursor)
        if not files:
            break

        for file in files:
            _delete_s3_files(file)
            if expire:
                file.mark_expired()
            else:
                db.delete(file)
            count += 1

        cursor = files[-1].id

    return count


def _delete_s3_files(file: File) -> None:
    """파일의 S3 오브젝트 삭제."""
    if file.file_key:
        try:
            _s3.delete_object(file.file_key)
        except Exception:
            logger.warning(
                "S3 파일 삭제 실패: key={key} file_id={file_id}",
                key=file.file_key,
                file_id=file.id,
            )


def _is_dwg_file(filename: str) -> bool:
    """파일명이 DWG 확장자인지 판별."""
    _, ext = os.path.splitext(filename)
    return ext.lower() in _DWG_EXTENSIONS


def _to_file_complete_response(file: File) -> FileCompleteResponse:
    return FileCompleteResponse(
        file_id=file.id,
        status=file.status,
        original_name=file.original_name,
        file_key=file.file_key,
        file_size=file.file_size,
        content_type=file.content_type,
        created_at=file.created_at,
    )
