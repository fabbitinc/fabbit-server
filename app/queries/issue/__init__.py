"""Issue 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.issue.get_timeline import get_timeline

__all__ = [
    "get_timeline",
]
