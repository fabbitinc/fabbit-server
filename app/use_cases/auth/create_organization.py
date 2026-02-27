"""기가입자의 조직 생성 — 조직 + 멤버십 + 테넌트 프로비저닝 + 토큰 발급."""

import uuid

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import CreateOrganizationRequest, CreateOrganizationResponse


@transactional()
def create_organization(
    db: Session,
    user_id: uuid.UUID,
    req: CreateOrganizationRequest,
) -> CreateOrganizationResponse:
    """조직 생성."""
    return auth_service.create_organization(db, user_id, req)
