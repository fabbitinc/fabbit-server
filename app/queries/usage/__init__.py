"""Usage 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.usage.get_storage_usage import get_storage_usage

__all__ = [
    "get_storage_usage",
]
