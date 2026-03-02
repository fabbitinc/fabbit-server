"""회원가입 — 인증 재검증 + 유저 + 조직 + 멤버십 + 프로비저닝 + 토큰 발급."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import RegisterRequest, RegisterResponse
from app.modules.organization import service as org_service
from app.modules.organization.constants import MembershipRole
from app.modules.organization.schemas import CreateOrganizationRequest, OrganizationResponse
from app.modules.user import service as user_service
from app.modules.user.schemas import UserResponse


@transactional()
def register(db: Session, req: RegisterRequest) -> RegisterResponse:
    """통합 회원가입: cross-domain 오케스트레이션."""
    # 1. Turnstile 봇 방지 검증
    auth_service.verify_turnstile(req.turnstile_token)

    # 2. 인증 재검증 + USED 처리
    email = auth_service.validate_and_consume_verification(
        db, req.verification_token, req.code
    )

    # 3. 유저 생성
    user = user_service.create_user(db, email, req.password, req.full_name)

    # 4. 조직 생성 + 멤버십(OWNER) + 프로비저닝
    org_req = CreateOrganizationRequest(
        org_name=req.org_name,
        slug=req.slug,
        industry=req.industry,
        team_size=req.team_size,
        plan_type=req.plan_type,
    )
    org = org_service.create_organization(db, user.id, org_req)

    # 5. 토큰 발급
    tokens = auth_service.issue_tokens(
        db, user.id, user.email, org.id, MembershipRole.OWNER
    )

    return RegisterResponse(
        user=UserResponse.model_validate(user),
        organization=OrganizationResponse.model_validate(org),
        tokens=tokens,
    )
