"""인증 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.auth import service
from app.modules.auth.schemas import (
    LoginRequest,
    LoginResponse,
    MeResponse,
    RefreshRequest,
    SignupRequest,
    SignupResponse,
    TokenResponse,
)

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/signup", response_model=SignupResponse)
def signup(req: SignupRequest, db: Session = Depends(get_db)):
    return service.signup(db, req)


@router.post("/login", response_model=LoginResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    return service.login(db, req)


@router.post("/refresh", response_model=TokenResponse)
def refresh(req: RefreshRequest, db: Session = Depends(get_db)):
    return service.refresh_tokens(db, req.refresh_token)


@router.post("/logout", status_code=204)
def logout(
    req: RefreshRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    service.logout(db, str(auth.account_id), req.refresh_token)


@router.get("/me", response_model=MeResponse)
def me(
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    return service.get_me(db, str(auth.account_id))
