"""인증 API 라우터."""

from fastapi import APIRouter, Depends, Query
from pydantic import EmailStr
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_origin_slug, require_auth, require_create_org_token
from app.core.auth_context import AuthContext, CreateOrgContext
from app.modules.auth import service
from app.modules.auth.schemas import (
    AcceptInvitationRequest,
    AcceptInvitationResponse,
    CheckEmailResponse,
    CheckSlugResponse,
    CreateOrganizationRequest,
    CreateOrganizationResponse,
    LoginRequest,
    LoginResponse,
    MeResponse,
    OrganizationResponse,
    PlanResponse,
    RefreshRequest,
    RegisterRequest,
    RegisterResponse,
    ScopedLoginResponse,
    SendVerificationRequest,
    SendVerificationResponse,
    SiteResponse,
    SwitchOrgRequest,
    TokenResponse,
    VerifyEmailRequest,
    VerifyEmailResponse,
    VerifyInvitationResponse,
)
from app.queries import invitation as invitation_queries
from app.use_cases import auth as auth_commands
from app.use_cases import invitation as invitation_commands

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.get("/site", response_model=SiteResponse)
def get_site(
    db: Session = Depends(get_db),
    slug: str | None = Depends(get_origin_slug),
):
    return service.get_site(db, slug)


@router.get("/plans", response_model=list[PlanResponse])
def get_plans():
    return service.get_plans()


@router.get("/check-email", response_model=CheckEmailResponse)
def check_email(email: EmailStr = Query(...), db: Session = Depends(get_db)):
    return service.check_email(db, email)


@router.get("/check-slug", response_model=CheckSlugResponse)
def check_slug(
    slug: str = Query(..., min_length=3, max_length=50),
    db: Session = Depends(get_db),
):
    return service.check_slug(db, slug)


# TODO 만료된 레코드 정리 필요
@router.post("/send-verification", response_model=SendVerificationResponse)
def send_verification(req: SendVerificationRequest, db: Session = Depends(get_db)):
    """이메일 인증코드 발송.

    입력된 이메일로 6자리 인증코드를 발송합니다.
    - 이미 가입된 이메일이면 에러
    - 60초 이내 재발송 요청 시 쿨다운 에러
    - 인증코드 유효시간: 10분
    """
    return auth_commands.send_verification(db, req)


@router.post("/verify-email", response_model=VerifyEmailResponse)
def verify_email(req: VerifyEmailRequest, db: Session = Depends(get_db)):
    """인증코드 검증.

    이메일로 받은 6자리 코드를 검증합니다.
    - 성공 시 verification_token을 반환 (회원가입 시 필요)
    - 5회 실패 시 코드 무효화 (재발송 필요)
    """
    return auth_commands.verify_email(db, req)


@router.post("/register", response_model=RegisterResponse)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    return service.register(db, req)


@router.post("/login", response_model=LoginResponse | ScopedLoginResponse)
def login(
    req: LoginRequest,
    db: Session = Depends(get_db),
    slug: str | None = Depends(get_origin_slug),
):
    """로그인.

    - **slug 있음** (서브도메인 접근): 해당 워크스페이스 멤버십 확인 → access+refresh 토큰 발급
    - **slug 없음** (루트 도메인 접근): 유저 인증만 → 조직 생성 전용 스코프 토큰 발급 (`scoped_token`)
    """
    return service.login(db, req, slug=slug)


@router.post("/refresh", response_model=TokenResponse)
def refresh(req: RefreshRequest, db: Session = Depends(get_db)):
    return service.refresh_tokens(db, req.refresh_token)


@router.post("/logout", status_code=204)
def logout(
    req: RefreshRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    service.logout(db, auth, req.refresh_token)


@router.post("/onboarding/complete", response_model=OrganizationResponse)
def complete_onboarding(
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    return service.complete_onboarding(db, auth)


@router.get("/me", response_model=MeResponse)
def me(
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    return service.get_me(db, auth)


@router.post("/organizations", response_model=CreateOrganizationResponse)
def create_organization(
    req: CreateOrganizationRequest,
    db: Session = Depends(get_db),
    ctx: CreateOrgContext = Depends(require_create_org_token),
):
    """기가입자 조직 생성.

    루트 도메인 로그인으로 발급받은 **스코프 토큰**(scope=create_org)이 필요합니다.
    조직 생성 + ADMIN 멤버십 + 테넌트 프로비저닝 후 정상 access+refresh 토큰을 반환합니다.
    """
    return auth_commands.create_organization(db, ctx.user_id, req)


@router.post("/switch-org", response_model=LoginResponse)
def switch_org(
    req: SwitchOrgRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """조직 전환.

    현재 인증된 유저가 다른 워크스페이스로 전환합니다.
    대상 워크스페이스의 멤버십을 확인한 후 새 access+refresh 토큰을 발급합니다.
    """
    return auth_commands.switch_org(db, auth.user_id, auth.email, req.slug)


@router.get("/invitations/verify", response_model=VerifyInvitationResponse)
def verify_invitation(
    token: str = Query(..., description="초대 토큰"),
    db: Session = Depends(get_db),
):
    """초대 토큰 검증.

    토큰의 유효성을 확인하고 초대 정보를 반환합니다.
    **is_existing_user**로 기가입 여부를 판단하여 프론트엔드에서 입력 폼을 분기합니다.
    """
    return invitation_queries.verify_invitation(db, token)


@router.post("/accept-invitation", response_model=AcceptInvitationResponse)
def accept_invitation(req: AcceptInvitationRequest, db: Session = Depends(get_db)):
    """초대 수락.

    토큰으로 초대를 조회하여 수락합니다.
    - **미가입자**: password, full_name 필수 → 유저 생성 + 조직 참여
    - **기가입자**: 조직 참여만 처리
    """
    return invitation_commands.accept_invitation(db, req)
