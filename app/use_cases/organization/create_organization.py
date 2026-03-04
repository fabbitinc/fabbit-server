"""기가입자의 조직 생성 — 조직 + 멤버십 + 테넌트 프로비저닝 + 토큰 발급."""

import uuid

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import CreateOrganizationResponse
from app.modules.organization import service as org_service
from app.modules.organization.constants import MembershipRole
from app.modules.organization.schemas import CreateOrganizationRequest, OrganizationResponse
from app.modules.subscription import service as subscription_service
from app.modules.user import service as user_service


@transactional()
def create_organization(
    db: Session,
    user_id: uuid.UUID,
    req: CreateOrganizationRequest,
) -> CreateOrganizationResponse:
    """조직 생성 + 토큰 발급."""
    # 유저 존재 확인
    user = user_service.get_user_or_raise(db, user_id)

    # 조직 + 멤버십(OWNER) + 프로비저닝
    org = org_service.create_organization(db, user.id, req)

    # 초기 구독 생성
    subscription_service.create_initial_subscription(db, org.id, org.plan_type)

    # 토큰 발급
    tokens = auth_service.issue_tokens(
        db, user.id, user.email, org.id, MembershipRole.OWNER
    )

    return CreateOrganizationResponse(
        organization=OrganizationResponse.model_validate(org),
        tokens=tokens,
    )
