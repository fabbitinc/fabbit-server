"""알림 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.notification import repository as notification_repo
from app.modules.notification.schemas import (
    NotificationListResponse,
    NotificationResponse,
)
from app.modules.user import mapper as user_mapper
from app.modules.user import repository as user_repo


@transactional(read_only=True)
def list_notifications(
    db: Session,
    auth: AuthContext,
    *,
    cursor: uuid.UUID | None = None,
    limit: int = 20,
    unread_only: bool = False,
) -> NotificationListResponse:
    """수신자별 알림 목록 cursor 기반 조회."""
    notifications = notification_repo.list_by_user_cursor(
        db, auth.user_id, cursor=cursor, limit=limit, unread_only=unread_only
    )
    items = [
        NotificationResponse.model_validate(n) for n in notifications
    ]
    next_cursor = notifications[-1].id if len(notifications) == limit else None

    # actor_id 수집 → 유저 정보 매핑
    actor_ids = {n.actor_id for n in notifications}
    users = user_repo.get_users_by_ids(db, list(actor_ids))
    user_map = {str(u.id): user_mapper.to_user_summary(u) for u in users}

    return NotificationListResponse(
        items=items, next_cursor=next_cursor, users=user_map
    )
