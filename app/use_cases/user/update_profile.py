"""사용자 프로필 수정."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.user_schemas import UpdateProfileRequest, UpdateProfileResponse


@transactional()
def update_profile(
    db: Session,
    auth: AuthContext,
    req: UpdateProfileRequest,
) -> UpdateProfileResponse:
    """프로필 수정 (partial update)."""
    return auth_service.update_profile(db, auth, req)
