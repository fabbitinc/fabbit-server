"""업로드 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.upload.schemas import (
    BatchCompleteRequest,
    BatchCompleteResponse,
    BatchCreateUploadRequest,
    BatchCreateUploadResponse,
    CreateUploadRequest,
    CreateUploadResponse,
    UploadCompleteResponse,
)
from app.modules.upload import service

router = APIRouter(prefix="/api/v1/uploads", tags=["uploads"])


@router.post("", response_model=CreateUploadResponse)
def create_upload(
    req: CreateUploadRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.create_upload(db, auth, req)


@router.post("/batch", response_model=BatchCreateUploadResponse)
def batch_create_uploads(
    req: BatchCreateUploadRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.batch_create_uploads(db, auth, req)


@router.post("/batch/complete", response_model=BatchCompleteResponse)
def batch_complete_uploads(
    req: BatchCompleteRequest,
    db: Session = Depends(get_tenant_db),
):
    return service.batch_complete_uploads(db, req)


@router.post("/{upload_id}/complete", response_model=UploadCompleteResponse)
def complete_upload(
    upload_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.complete_upload(db, upload_id, auth)
