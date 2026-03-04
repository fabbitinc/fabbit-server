"""Dashboard 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.dashboard.get_stats import get_stats
from app.queries.dashboard.get_storage_usage import get_storage_usage

__all__ = [
    "get_stats",
    "get_storage_usage",
]
