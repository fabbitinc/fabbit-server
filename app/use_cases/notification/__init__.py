"""Notification 유스케이스 — 쓰기 오케스트레이션 re-export."""

from app.use_cases.notification.mark_all_as_read import mark_all_as_read
from app.use_cases.notification.mark_as_read import mark_as_read

__all__ = [
    "mark_as_read",
    "mark_all_as_read",
]
