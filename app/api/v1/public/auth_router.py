"""인증 API 라우터."""

from fastapi import APIRouter, Depends, Query
from pydantic import EmailStr
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_origin_slug, require_auth
from app.core.auth_context import AuthContext
from app.modules.auth import service
from app.modules.auth.schemas import (
    AcceptInvitationRequest,
    AcceptInvitationResponse,
    CheckEmailResponse,
    CheckSlugResponse,
    LoginRequest,
    LoginResponse,
    MeResponse,
    OrganizationResponse,
    PlanResponse,
    RefreshRequest,
    RegisterRequest,
    RegisterResponse,
    SiteResponse,
    TokenResponse,
    VerifyInvitationResponse,
)
from app.queries import invitation as invitation_queries
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


@router.post("/register", response_model=RegisterResponse)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    return service.register(db, req)


@router.post("/login", response_model=LoginResponse)
def login(
    req: LoginRequest,
    db: Session = Depends(get_db),
    slug: str | None = Depends(get_origin_slug),
):
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
