"""Activation 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.activation.get_starters import get_starters

__all__ = [
    "get_starters",
]
