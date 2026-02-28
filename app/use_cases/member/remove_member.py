"""조직에서 멤버 제거."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service


@transactional()
def remove_member(
    db: Session,
    auth: AuthContext,
    user_id: uuid.UUID,
) -> None:
    """조직에서 멤버 제거."""
    auth_service.remove_member(db, auth, user_id)
