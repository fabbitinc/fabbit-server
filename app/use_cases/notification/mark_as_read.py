"""알림 단건 읽음 처리."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.notification import service as notification_service


@transactional
def mark_as_read(
    db: Session,
    auth: AuthContext,
    notification_id: uuid.UUID,
) -> None:
    """알림 단건 읽음 처리."""
    notification_service.mark_as_read(db, auth.user_id, notification_id)
