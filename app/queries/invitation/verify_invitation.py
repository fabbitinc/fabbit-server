"""초대 토큰 검증 — 프론트엔드에서 수락 화면 분기용."""

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.auth import repository as repo
from app.modules.auth.constants import InvitationStatus
from app.modules.auth.models import _hash_token
from app.modules.auth.schemas import VerifyInvitationResponse


@transactional(read_only=True)
def verify_invitation(
    db: Session,
    token: str,
) -> VerifyInvitationResponse:
    """토큰으로 초대 정보 조회 + 기가입 여부 반환."""
    token_hash = _hash_token(token)
    invitation = repo.get_invitation_by_token_hash(db, token_hash)
    if not invitation:
        raise AppError(message="유효하지 않은 초대입니다", code="NOT_FOUND")

    if invitation.status != InvitationStatus.PENDING:
        raise AppError(message="이미 처리된 초대입니다", code="VALIDATION_ERROR")

    if invitation.is_expired:
        raise AppError(message="만료된 초대입니다", code="VALIDATION_ERROR")

    org = repo.get_org_by_id(db, invitation.org_id)
    inviter = repo.get_user_by_id(db, invitation.invited_by)
    existing_user = repo.get_user_by_email(db, invitation.email)

    return VerifyInvitationResponse(
        email=invitation.email,
        org_name=org.name,
        inviter_name=inviter.full_name,
        role=invitation.role,
        is_existing_user=existing_user is not None,
        expires_at=invitation.expires_at,
    )
