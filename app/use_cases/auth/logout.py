"""로그아웃 — 리프레시 토큰 폐기."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service


@transactional()
def logout(db: Session, auth: AuthContext, refresh_token_str: str) -> None:
    """로그아웃: 리프레시 토큰 폐기."""
    auth_service.logout(db, auth, refresh_token_str)
