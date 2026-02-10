"""업로드 API 라우터.

Presigned URL 발급 및 업로드 완료 확인 엔드포인트.
비즈니스 로직이 단순하므로 라우터에 인라인으로 구현합니다.
"""

import uuid

from fastapi import APIRouter, Depends
from loguru import logger
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.infrastructure.s3_client import S3Client
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

router = APIRouter(prefix="/uploads", tags=["uploads"])

_s3 = S3Client()


# ── 단일 엔드포인트 ──


@router.post("", response_model=CreateUploadResponse)
def create_upload(
    req: CreateUploadRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Presigned URL 발급 + Upload 레코드 생성."""
    upload_id = generate_uuid7()
    file_key = f"tenants/{auth.org_id}/raw_data/{upload_id}/{req.original_name}"

    upload = Upload(
        id=upload_id,
        original_name=req.original_name,
        file_key=file_key,
        content_type=req.content_type,
        file_size=req.file_size,
        project_id=req.project_id,
    )
    db.add(upload)

    presigned = _s3.generate_upload_presigned_url(
        file_key=file_key,
        content_type=req.content_type,
        content_length=req.file_size,
    )

    db.commit()

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


# ── 배치 엔드포인트 (고정 경로 — 동적 경로보다 먼저 등록) ──


@router.post("/batch", response_model=BatchCreateUploadResponse)
def batch_create_uploads(
    req: BatchCreateUploadRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """여러 파일의 Presigned URL 일괄 발급."""
    results: list[CreateUploadResponse] = []

    for item in req.items:
        upload_id = generate_uuid7()
        file_key = f"tenants/{auth.org_id}/raw_data/{upload_id}/{item.original_name}"

        upload = Upload(
            id=upload_id,
            original_name=item.original_name,
            file_key=file_key,
            content_type=item.content_type,
            file_size=item.file_size,
            project_id=item.project_id,
        )
        db.add(upload)

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

    db.commit()

    logger.info(
        "배치 업로드 URL 발급: {count}건",
        count=len(results),
    )

    return BatchCreateUploadResponse(items=results)


@router.post("/batch/complete", response_model=BatchCompleteResponse)
def batch_complete_uploads(
    req: BatchCompleteRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """여러 업로드의 완료를 일괄 확인."""
    completed: list[UploadCompleteResponse] = []
    failed: list[BatchCompleteFailure] = []

    uploads = db.query(Upload).filter(Upload.id.in_(req.upload_ids)).all()
    upload_map = {u.id: u for u in uploads}

    for uid in req.upload_ids:
        upload = upload_map.get(uid)
        if upload is None:
            failed.append(
                BatchCompleteFailure(upload_id=uid, reason="업로드를 찾을 수 없습니다")
            )
            continue

        if upload.status == "UPLOADED":
            failed.append(
                BatchCompleteFailure(upload_id=uid, reason="이미 완료된 업로드입니다")
            )
            continue

        obj_meta = _s3.head_object(upload.file_key)
        if obj_meta is None:
            failed.append(
                BatchCompleteFailure(
                    upload_id=uid, reason="S3에 파일이 존재하지 않습니다"
                )
            )
            continue

        upload.status = "UPLOADED"
        completed.append(
            UploadCompleteResponse(
                upload_id=upload.id,
                status=upload.status,
                original_name=upload.original_name,
                file_key=upload.file_key,
                file_size=upload.file_size,
                content_type=upload.content_type,
                created_at=upload.created_at,
            )
        )

    db.commit()

    logger.info(
        "배치 업로드 완료: 성공={ok}건 실패={fail}건",
        ok=len(completed),
        fail=len(failed),
    )

    return BatchCompleteResponse(items=completed, failed=failed)


# ── 동적 경로 (배치 고정 경로 뒤에 등록) ──


@router.post("/{upload_id}/complete", response_model=UploadCompleteResponse)
def complete_upload(
    upload_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """업로드 완료 확인 (S3 head_object로 검증 후 상태 변경)."""
    upload = db.query(Upload).filter(Upload.id == upload_id).first()
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    if upload.status == "UPLOADED":
        raise AppError(message="이미 완료된 업로드입니다", code="CONFLICT")

    # S3에 실제로 파일이 존재하는지 검증
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

    return UploadCompleteResponse(
        upload_id=upload.id,
        status=upload.status,
        original_name=upload.original_name,
        file_key=upload.file_key,
        file_size=upload.file_size,
        content_type=upload.content_type,
        created_at=upload.created_at,
    )
