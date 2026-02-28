"""인증 API 라우터."""

from fastapi import APIRouter, Depends, Query
from pydantic import EmailStr
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_origin_slug, require_auth
from app.core.auth_context import AuthContext
from app.modules.auth.schemas import (
    AcceptInvitationRequest,
    AcceptInvitationResponse,
    CheckEmailResponse,
    LoginRequest,
    LoginResponse,
    RefreshRequest,
    RegisterRequest,
    RegisterResponse,
    ScopedLoginResponse,
    SendVerificationRequest,
    SendVerificationResponse,
    TokenResponse,
    VerifyEmailRequest,
    VerifyEmailResponse,
    VerifyInvitationResponse,
)
from app.modules.organization.schemas import (
    CheckSlugResponse,
    PlanResponse,
    SiteResponse,
)
from app.queries import auth as auth_queries
from app.queries import invitation as invitation_queries
from app.use_cases import auth as auth_commands
from app.use_cases import invitation as invitation_commands

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.get("/site", response_model=SiteResponse)
def get_site(
    db: Session = Depends(get_db),
    slug: str | None = Depends(get_origin_slug),
):
    """서브도메인 워크스페이스 정보 조회."""
    return auth_queries.get_site(db, slug)


@router.get("/plans", response_model=list[PlanResponse])
def get_plans():
    """플랜 목록 및 제한값 조회."""
    return auth_queries.get_plans()


@router.get("/check-slug", response_model=CheckSlugResponse)
def check_slug(
    slug: str = Query(..., min_length=3, max_length=50),
    db: Session = Depends(get_db),
):
    """slug 사용 가능 여부 확인."""
    return auth_queries.check_slug(db, slug)


@router.get("/check-email", response_model=CheckEmailResponse)
def check_email(email: EmailStr = Query(...), db: Session = Depends(get_db)):
    """이메일 중복 확인."""
    return auth_queries.check_email(db, email)


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
    """회원가입.

    이메일 인증 완료 후 유저 + 조직 생성 + 토큰 발급.
    """
    return auth_commands.register(db, req)


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
    return auth_commands.login(db, req, slug=slug)


@router.post("/refresh", response_model=TokenResponse)
def refresh(req: RefreshRequest, db: Session = Depends(get_db)):
    """토큰 갱신."""
    return auth_commands.refresh_tokens(db, req.refresh_token)


@router.post("/logout", status_code=204)
def logout(
    req: RefreshRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """로그아웃."""
    auth_commands.logout(db, auth, req.refresh_token)


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
