"""Notification 도메인 Service."""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.notification import repository as notification_repo


def mark_as_read(db: Session, user_id: uuid.UUID, notification_id: uuid.UUID) -> None:
    """알림 단건 읽음 처리. 대상 없으면 AppError."""
    success = notification_repo.mark_as_read(db, user_id, notification_id)
    if not success:
        raise AppError(message="알림을 찾을 수 없습니다", code="NOT_FOUND")


def mark_all_as_read(db: Session, user_id: uuid.UUID) -> None:
    """수신자의 모든 미읽음 알림을 읽음 처리."""
    notification_repo.mark_all_as_read(db, user_id)
