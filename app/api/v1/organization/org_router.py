"""조직 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_auth, require_create_org_token
from app.core.auth_context import AuthContext, CreateOrgContext
from app.modules.auth.schemas import CreateOrganizationResponse, LoginResponse
from app.modules.organization.schemas import (
    CreateOrganizationRequest,
    SwitchOrgRequest,
)
from app.use_cases import organization as org_commands

router = APIRouter(prefix="/api/v1/organizations", tags=["organizations"])


@router.post("", response_model=CreateOrganizationResponse)
def create_organization(
    req: CreateOrganizationRequest,
    db: Session = Depends(get_db),
    ctx: CreateOrgContext = Depends(require_create_org_token),
):
    """기가입자 조직 생성.

    루트 도메인 로그인으로 발급받은 **스코프 토큰**(scope=create_org)이 필요합니다.
    조직 생성 + ADMIN 멤버십 + 테넌트 프로비저닝 후 정상 access+refresh 토큰을 반환합니다.
    """
    return org_commands.create_organization(db, ctx.user_id, req)


@router.post("/switch", response_model=LoginResponse)
def switch_org(
    req: SwitchOrgRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """조직 전환.

    현재 인증된 유저가 다른 워크스페이스로 전환합니다.
    대상 워크스페이스의 멤버십을 확인한 후 새 access+refresh 토큰을 발급합니다.
    """
    return org_commands.switch_org(db, auth.user_id, auth.email, req.slug)
