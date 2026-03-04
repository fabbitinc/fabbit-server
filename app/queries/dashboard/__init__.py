"""Dashboard 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.dashboard.get_stats import get_stats

__all__ = [
    "get_stats",
]
