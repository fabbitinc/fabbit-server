"""초대 생성 — 검증 + 초대 레코드 생성 + 이메일 발송."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import CreateInvitationRequest, InvitationResponse


@transactional()
def create_invitation(
    db: Session,
    auth: AuthContext,
    req: CreateInvitationRequest,
) -> InvitationResponse:
    """초대 생성 및 이메일 발송."""
    return auth_service.create_invitation(db, auth, req)
