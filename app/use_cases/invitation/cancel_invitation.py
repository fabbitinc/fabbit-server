"""초대 취소 — PENDING 상태 초대를 CANCELLED로 변경."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service


@transactional()
def cancel_invitation(
    db: Session,
    auth: AuthContext,
    invitation_id: uuid.UUID,
) -> None:
    """초대 취소."""
    auth_service.cancel_invitation(db, auth, invitation_id)
