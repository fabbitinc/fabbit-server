"""Notification 도메인 Repository."""

import uuid
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.modules.notification.models import Notification


def list_by_user_cursor(
    db: Session,
    user_id: uuid.UUID,
    *,
    cursor: uuid.UUID | None = None,
    limit: int = 20,
    unread_only: bool = False,
) -> list[Notification]:
    """수신자별 알림 cursor 기반 조회 (최신순).

    cursor는 이전 페이지 마지막 항목의 id.
    UUID v7이 시간순이므로 id < cursor로 이전 항목을 가져온다.
    """
    query = db.query(Notification).filter(Notification.user_id == user_id)
    if cursor is not None:
        query = query.filter(Notification.id < cursor)
    if unread_only:
        query = query.filter(Notification.read_at.is_(None))
    return query.order_by(Notification.id.desc()).limit(limit).all()


def count_unread(db: Session, user_id: uuid.UUID) -> int:
    """미읽음 알림 개수."""
    return (
        db.query(Notification)
        .filter(Notification.user_id == user_id, Notification.read_at.is_(None))
        .count()
    )


def mark_as_read(
    db: Session, user_id: uuid.UUID, notification_id: uuid.UUID
) -> bool:
    """단건 읽음 처리. 성공 시 True, 대상 없으면 False."""
    count = (
        db.query(Notification)
        .filter(
            Notification.id == notification_id,
            Notification.user_id == user_id,
            Notification.read_at.is_(None),
        )
        .update({"read_at": datetime.now(timezone.utc)})
    )
    return count > 0


def mark_all_as_read(db: Session, user_id: uuid.UUID) -> int:
    """전체 읽음 처리 (bulk update). 업데이트된 행 수 반환."""
    return (
        db.query(Notification)
        .filter(Notification.user_id == user_id, Notification.read_at.is_(None))
        .update({"read_at": datetime.now(timezone.utc)})
    )
