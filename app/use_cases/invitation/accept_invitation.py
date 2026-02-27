"""초대 수락 — 미가입 시 유저 생성 + 조직 참여 + 토큰 발급."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import AcceptInvitationRequest, AcceptInvitationResponse


@transactional()
def accept_invitation(
    db: Session,
    req: AcceptInvitationRequest,
) -> AcceptInvitationResponse:
    """초대 수락."""
    return auth_service.accept_invitation(db, req)
