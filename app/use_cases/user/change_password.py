"""비밀번호 변경."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.user_schemas import ChangePasswordRequest


@transactional()
def change_password(
    db: Session,
    auth: AuthContext,
    req: ChangePasswordRequest,
) -> None:
    """현재 비밀번호 검증 후 새 비밀번호로 변경."""
    auth_service.change_password(db, auth, req)
