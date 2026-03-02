"""Notification 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.notification.count_unread import count_unread
from app.queries.notification.list_notifications import list_notifications

__all__ = [
    "count_unread",
    "list_notifications",
]
