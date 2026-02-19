"""매핑 API 라우터."""

import json
import uuid
from pathlib import Path

from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse
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
    MappingUpdateRequest,
    MappingValidateRequest,
    MappingValidateResponse,
)

router = APIRouter(prefix="/api/v1/mappings", tags=["mappings"])


@router.post("/preview", response_model=MappingPreviewResponse)
def preview_mapping(
    req: MappingPreviewRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    # TODO: 프론트 개발용 임시 스텁 — 완료 후 제거
    _STUB_PREVIEW = (
        Path(__file__).resolve().parents[4] / "sample" / "mapping_preview_response.json"
    )
    if _STUB_PREVIEW.exists():
        return JSONResponse(content=json.loads(_STUB_PREVIEW.read_text()))
    return service.preview_mapping(db, auth, req)


@router.post("/confirm", response_model=MappingResponse)
def confirm_mapping(
    req: MappingConfirmRequest,
    db: Session = Depends(get_tenant_db),
):
    return service.confirm_mapping(db, req)


@router.post("/validate", response_model=MappingValidateResponse)
def validate_mapping(
    req: MappingValidateRequest,
    db: Session = Depends(get_tenant_db),
):
    return service.validate_mapping(db, req)


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


@router.put("/{mapping_id}", response_model=MappingResponse)
def update_mapping(
    mapping_id: uuid.UUID,
    req: MappingUpdateRequest,
    db: Session = Depends(get_tenant_db),
):
    return service.update_mapping(db, mapping_id, req)


@router.delete("/{mapping_id}", status_code=204)
def delete_mapping(
    mapping_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    service.deactivate_mapping(db, mapping_id)
