"""매핑 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.mapping.schemas import (
    MappingConfirmRequest,
    MappingListResponse,
    MappingPreviewRequest,
    MappingPreviewResponse,
    MappingResponse,
    MappingUpdateRequest,
    MappingValidateRequest,
    MappingValidateResponse,
)
from app.queries import mapping as mapping_queries
from app.use_cases import mapping as mapping_commands

router = APIRouter(prefix="/api/v1/mappings", tags=["mappings"])


@router.post("/preview", response_model=MappingPreviewResponse)
def preview_mapping(
    req: MappingPreviewRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return mapping_commands.preview_mapping(db, auth, req)


@router.post("/confirm", response_model=MappingResponse)
def confirm_mapping(
    req: MappingConfirmRequest,
    db: Session = Depends(get_tenant_db),
):
    return mapping_commands.confirm_mapping(db, req)


@router.post("/validate", response_model=MappingValidateResponse)
def validate_mapping(
    req: MappingValidateRequest,
    db: Session = Depends(get_tenant_db),
):
    return mapping_commands.validate_mapping(db, req)


@router.get("", response_model=MappingListResponse)
def list_mappings(
    db: Session = Depends(get_tenant_db),
):
    return mapping_queries.list_mappings(db)


@router.get("/{mapping_id}", response_model=MappingResponse)
def get_mapping(
    mapping_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    return mapping_queries.get_mapping(db, mapping_id)


@router.put("/{mapping_id}", response_model=MappingResponse)
def update_mapping(
    mapping_id: uuid.UUID,
    req: MappingUpdateRequest,
    db: Session = Depends(get_tenant_db),
):
    return mapping_commands.update_mapping(db, mapping_id, req)


@router.delete("/{mapping_id}", status_code=204)
def delete_mapping(
    mapping_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    mapping_commands.deactivate_mapping(db, mapping_id)
