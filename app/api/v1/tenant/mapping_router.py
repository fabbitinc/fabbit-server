"""매핑 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.mapping import service
from app.modules.mapping.schemas import (
    MappingConfirmRequest,
    MappingListResponse,
    MappingPreviewRequest,
    MappingPreviewResponse,
    MappingResponse,
)

router = APIRouter(prefix="/api/v1/mappings", tags=["mappings"])


@router.post("/preview", response_model=MappingPreviewResponse)
def preview_mapping(
    req: MappingPreviewRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.preview_mapping(db, auth, req)


@router.post("/confirm", response_model=MappingResponse)
def confirm_mapping(
    req: MappingConfirmRequest,
    db: Session = Depends(get_tenant_db),
):
    return service.confirm_mapping(db, req)


@router.get("", response_model=MappingListResponse)
def list_mappings(
    db: Session = Depends(get_tenant_db),
):
    return service.list_mappings(db)


@router.get("/{mapping_id}", response_model=MappingResponse)
def get_mapping(
    mapping_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    return service.get_mapping(db, mapping_id)
