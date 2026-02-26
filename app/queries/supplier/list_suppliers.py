"""공급사 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.supplier import repository as repo
from app.modules.supplier.schemas import SupplierListResponse, SupplierSummary


@transactional(read_only=True)
def list_suppliers(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> SupplierListResponse:
    """Supplier 목록 페이징 조회."""
    suppliers, total = repo.list_suppliers_paginated(
        db, search=search, offset=offset, limit=limit
    )

    items = [
        SupplierSummary(
            id=s.id,
            company_name=s.company_name,
            code=s.code,
            country=s.country,
        )
        for s in suppliers
    ]

    return SupplierListResponse(total=total, offset=offset, limit=limit, items=items)
