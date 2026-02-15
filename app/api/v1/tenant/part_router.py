"""부품(Part) 조회 API 라우터."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.part import service
from app.modules.part.schemas import (
    BomTreeResponse,
    PartDetailResponse,
    PartListResponse,
)

router = APIRouter(prefix="/api/v1/parts", tags=["parts"])


@router.get("", response_model=PartListResponse)
def list_parts(
    search: str | None = Query(None, description="part_number 또는 name 검색"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.list_parts(db, auth, search=search, offset=offset, limit=limit)


@router.get("/{part_number}", response_model=PartDetailResponse)
def get_part(
    part_number: str,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.get_part(db, auth, part_number)


@router.get("/{part_number}/bom-tree", response_model=BomTreeResponse)
def get_part_bom_tree(
    part_number: str,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.get_part_bom_tree(db, auth, part_number)
