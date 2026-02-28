"""초대 수락 — 미가입 시 유저 생성 + 조직 참여 + 토큰 발급."""

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.auth import repository as auth_repo
from app.modules.auth import service as auth_service
from app.modules.auth.constants import InvitationStatus
from app.modules.auth.models import _hash_token
from app.modules.auth.schemas import AcceptInvitationRequest, AcceptInvitationResponse
from app.modules.organization import repository as org_repo
from app.modules.organization import service as org_service
from app.modules.organization.schemas import OrganizationResponse
from app.modules.user import repository as user_repo
from app.modules.user import service as user_service
from app.modules.user.schemas import UserResponse


@transactional()
def accept_invitation(
    db: Session,
    req: AcceptInvitationRequest,
) -> AcceptInvitationResponse:
    """초대 수락: cross-domain 오케스트레이션."""
    token_hash = _hash_token(req.token)
    invitation = auth_repo.get_invitation_by_token_hash(db, token_hash)
    if not invitation:
        raise AppError(message="유효하지 않은 초대입니다", code="NOT_FOUND")

    if invitation.status != InvitationStatus.PENDING:
        raise AppError(message="이미 처리된 초대입니다", code="VALIDATION_ERROR")

    if invitation.is_expired:
        raise AppError(message="만료된 초대입니다", code="VALIDATION_ERROR")

    # 기존 유저 확인
    user = user_repo.get_user_by_email(db, invitation.email)
    is_new_user = user is None

    if is_new_user:
        # 미가입자 — password, full_name 필수
        if not req.password or not req.full_name:
            raise AppError(
                message="신규 가입 시 비밀번호와 이름이 필요합니다",
                code="VALIDATION_ERROR",
            )
        user = user_service.create_user(db, invitation.email, req.password, req.full_name)

    # 이미 멤버인지 확인
    existing_membership = org_repo.get_membership(db, user.id, invitation.org_id)
    if existing_membership:
        invitation.accept()
        raise AppError(message="이미 조직에 소속된 멤버입니다", code="ALREADY_EXISTS")

    # 멤버십 생성
    org_service.add_member(db, user.id, invitation.org_id, invitation.role)

    # 초대 수락 처리
    invitation.accept()

    # 토큰 발급
    org = org_repo.get_org_by_id(db, invitation.org_id)
    tokens = auth_service.issue_tokens(
        db, user.id, user.email, invitation.org_id, invitation.role
    )

    return AcceptInvitationResponse(
        user=UserResponse.model_validate(user),
        organization=OrganizationResponse.model_validate(org),
        tokens=tokens,
        is_new_user=is_new_user,
    )
