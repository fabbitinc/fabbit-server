"""조직 전환 — 멤버십 확인 + 새 토큰 발급."""

import uuid

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import LoginResponse
from app.modules.organization import service as org_service
from app.modules.user import service as user_service
from app.modules.user.schemas import UserResponse


@transactional()
def switch_org(
    db: Session,
    user_id: uuid.UUID,
    email: str,
    slug: str,
) -> LoginResponse:
    """조직 전환: 멤버십 확인 + 토큰 발급."""
    membership = org_service.switch_org(db, user_id, slug)
    user = user_service.get_user_or_raise(db, user_id)

    tokens = auth_service.issue_tokens(
        db, user.id, email, membership.org_id, membership.role
    )

    return LoginResponse(
        user=UserResponse.model_validate(user),
        tokens=tokens,
    )
