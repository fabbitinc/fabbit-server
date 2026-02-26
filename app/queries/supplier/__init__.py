"""Supplier 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.supplier.list_suppliers import list_suppliers

__all__ = [
    "list_suppliers",
]
