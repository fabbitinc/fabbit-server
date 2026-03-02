"""알림 전체 읽음 처리."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.notification import service as notification_service


@transactional
def mark_all_as_read(
    db: Session,
    auth: AuthContext,
) -> None:
    """수신자의 모든 미읽음 알림을 읽음 처리."""
    notification_service.mark_all_as_read(db, auth.user_id)
