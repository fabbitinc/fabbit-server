"""멤버 역할 변경."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.organization import service as org_service
from app.modules.organization.models import Membership


@transactional()
def change_member_role(
    db: Session,
    auth: AuthContext,
    user_id: uuid.UUID,
    new_role: str,
) -> Membership:
    """멤버 역할 변경."""
    return org_service.change_member_role(db, auth, user_id, new_role)
