"""공급사(Supplier) 조회 API 라우터."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.supplier.schemas import SupplierListResponse
from app.queries import supplier as supplier_queries

router = APIRouter(prefix="/api/v1/suppliers", tags=["suppliers"])


@router.get("", response_model=SupplierListResponse)
def list_suppliers(
    search: str | None = Query(None, description="company_name 또는 code 검색"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """공급사 목록 조회.

    company_name, code로 ILIKE 검색을 지원합니다.
    """
    return supplier_queries.list_suppliers(db, auth, search=search, offset=offset, limit=limit)
