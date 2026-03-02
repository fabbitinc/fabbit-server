"""미읽음 알림 개수 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.notification import repository as notification_repo
from app.modules.notification.schemas import UnreadCountResponse


@transactional(read_only=True)
def count_unread(
    db: Session,
    auth: AuthContext,
) -> UnreadCountResponse:
    """미읽음 알림 개수."""
    count = notification_repo.count_unread(db, auth.user_id)
    return UnreadCountResponse(count=count)
