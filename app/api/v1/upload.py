"""업로드 API 라우터.

Presigned URL 발급 및 업로드 완료 확인 엔드포인트.
비즈니스 로직이 단순하므로 라우터에 인라인으로 구현합니다.
"""

import uuid

from fastapi import APIRouter, Depends
from loguru import logger
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.api.deps import get_current_org_id, require_auth
from app.core.auth_context import AuthContext
from app.core.database import SessionLocal
from app.core.exceptions import AppError
from app.infrastructure.s3_client import S3Client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.upload.models import Upload
from app.modules.upload.schemas import (
    CreateUploadRequest,
    CreateUploadResponse,
    UploadCompleteResponse,
)

router = APIRouter(prefix="/uploads", tags=["uploads"])

_s3 = S3Client()


def _get_tenant_db(org_id: uuid.UUID = Depends(get_current_org_id)):
    """테넌트 격리 세션 의존성."""
    schema = org_id_to_schema(org_id)
    db = SessionLocal()
    try:
        db.execute(text(f"SET search_path = {schema}, ag_catalog, public"))
        yield db
    finally:
        db.close()


@router.post("", response_model=CreateUploadResponse)
def create_upload(
    req: CreateUploadRequest,
    auth: AuthContext = Depends(require_auth),
    org_id: uuid.UUID = Depends(get_current_org_id),
    db: Session = Depends(_get_tenant_db),
):
    """Presigned URL 발급 + Upload 레코드 생성."""
    upload = Upload(
        original_name=req.original_name,
        content_type=req.content_type,
        file_size=req.file_size,
        project_id=req.project_id,
    )
    db.add(upload)
    db.flush()

    file_key = f"tenants/{org_id}/raw_data/{upload.id}/{req.original_name}"
    upload.file_key = file_key

    presigned = _s3.generate_upload_presigned_url(
        file_key=file_key,
        content_type=req.content_type,
        content_length=req.file_size,
    )

    db.commit()

    logger.info(
        "업로드 URL 발급: upload_id={upload_id} file_key={file_key}",
        upload_id=upload.id,
        file_key=file_key,
    )

    return CreateUploadResponse(
        upload_id=upload.id,
        upload_url=presigned["upload_url"],
        file_key=file_key,
    )


@router.post("/{upload_id}/complete", response_model=UploadCompleteResponse)
def complete_upload(
    upload_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(_get_tenant_db),
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
