"""파일 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.file.schemas import (
    BatchCompleteRequest,
    BatchCompleteResponse,
    BatchCreateFileRequest,
    BatchCreateFileResponse,
    CreateFileRequest,
    CreateFileResponse,
    FileCompleteResponse,
)
from app.use_cases import file as file_commands

router = APIRouter(prefix="/api/v1/files", tags=["files"])


@router.post("/upload", response_model=CreateFileResponse)
def create_file(
    req: CreateFileRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return file_commands.create_file(db, auth, req)


@router.post("/upload/batch", response_model=BatchCreateFileResponse)
def batch_create_files(
    req: BatchCreateFileRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return file_commands.batch_create_files(db, auth, req)


@router.post("/upload/batch/complete", response_model=BatchCompleteResponse)
def batch_complete_files(
    req: BatchCompleteRequest,
    db: Session = Depends(get_tenant_db),
):
    return file_commands.batch_complete_files(db, req)


@router.post("/upload/{file_id}/complete", response_model=FileCompleteResponse)
def complete_file(
    file_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return file_commands.complete_file(db, file_id, auth)
