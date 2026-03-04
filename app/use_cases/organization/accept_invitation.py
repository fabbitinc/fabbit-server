"""초대 수락 — 미가입 시 유저 생성 + 조직 참여 + 토큰 발급."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import AcceptInvitationRequest, AcceptInvitationResponse
from app.modules.organization import service as org_service
from app.modules.organization.schemas import OrganizationResponse
from app.modules.user import service as user_service
from app.modules.user.schemas import UserResponse


@transactional()
def accept_invitation(
    db: Session,
    req: AcceptInvitationRequest,
) -> AcceptInvitationResponse:
    """초대 수락: cross-domain 오케스트레이션."""
    invitation = auth_service.validate_invitation_token(db, req.token)

    # 기존 유저 조회 또는 신규 생성
    user, is_new_user = user_service.find_or_create_for_invitation(
        db, invitation.email, req.password, req.full_name
    )

    # 멤버십 생성 (중복 시 에러, 좌석 예약 포함)
    org_service.add_member(db, user.id, invitation.org_id, invitation.role)

    # 초대 수락 처리
    invitation.accept()

    # 토큰 발급
    org = org_service.get_org_or_raise(db, invitation.org_id)
    tokens = auth_service.issue_tokens(
        db, user.id, user.email, invitation.org_id, invitation.role
    )

    return AcceptInvitationResponse(
        user=UserResponse.model_validate(user),
        organization=OrganizationResponse.model_validate(org),
        tokens=tokens,
        is_new_user=is_new_user,
    )
