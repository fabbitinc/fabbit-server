"""공급사(Supplier) API Pydantic 스키마."""

import uuid

from pydantic import BaseModel


class SupplierSummary(BaseModel):
    id: uuid.UUID
    company_name: str
    code: str | None = None
    country: str | None = None


class SupplierListResponse(BaseModel):
    total: int
    offset: int
    limit: int
    items: list[SupplierSummary]
