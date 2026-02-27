"""조직 전환 — 대상 조직 멤버십 확인 + 새 토큰 발급."""

import uuid

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import LoginResponse


@transactional()
def switch_org(
    db: Session,
    user_id: uuid.UUID,
    email: str,
    slug: str,
) -> LoginResponse:
    """조직 전환."""
    return auth_service.switch_org(db, user_id, email, slug)
