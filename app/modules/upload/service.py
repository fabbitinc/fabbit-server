"""업로드 도메인 서비스 레이어."""

import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.s3_client import S3Client
from app.modules.upload import repository as repo
from app.modules.upload.models import Upload
from app.modules.upload.schemas import (
    BatchCompleteFailure,
    BatchCompleteRequest,
    BatchCompleteResponse,
    BatchCreateUploadRequest,
    BatchCreateUploadResponse,
    CreateUploadRequest,
    CreateUploadResponse,
    UploadCompleteResponse,
)

_s3 = S3Client()


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
        project_id=req.project_id,
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
            project_id=item.project_id,
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

        if upload.status == "UPLOADED":
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

        upload.status = "UPLOADED"
        completed.append(_to_upload_complete_response(upload))

    db.commit()
    logger.info(
        "배치 업로드 완료: 성공={ok}건 실패={fail}건",
        ok=len(completed),
        fail=len(failed),
    )
    return BatchCompleteResponse(items=completed, failed=failed)


def complete_upload(
    db: Session,
    upload_id: uuid.UUID,
) -> UploadCompleteResponse:
    upload = repo.get_upload_by_id(db, upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    if upload.status == "UPLOADED":
        raise AppError(message="이미 완료된 업로드입니다", code="CONFLICT")

    obj_meta = _s3.head_object(upload.file_key)
    if obj_meta is None:
        raise AppError(
            message="S3에 파일이 존재하지 않습니다. 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    upload.status = "UPLOADED"
    db.commit()

    logger.info(
        "업로드 완료: upload_id={upload_id} size={size}",
        upload_id=upload.id,
        size=obj_meta["content_length"],
    )
    return _to_upload_complete_response(upload)


def _to_upload_complete_response(upload: Upload) -> UploadCompleteResponse:
    return UploadCompleteResponse(
        upload_id=upload.id,
        status=upload.status,
        original_name=upload.original_name,
        file_key=upload.file_key,
        file_size=upload.file_size,
        content_type=upload.content_type,
        created_at=upload.created_at,
    )
