"""초대 생성 — 검증 + 초대 레코드 생성 + 이메일 발송."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import CreateInvitationRequest, InvitationResponse
from app.modules.organization import service as org_service
from app.modules.user import service as user_service


@transactional()
def create_invitation(
    db: Session,
    auth: AuthContext,
    req: CreateInvitationRequest,
) -> InvitationResponse:
    """초대 생성 및 이메일 발송."""
    # 이미 조직 멤버인지 확인
    existing_user = user_service.get_user_by_email(db, req.email)
    if existing_user:
        org_service.check_not_member_by_email(db, auth.org_id, existing_user.id)

    # 초대 레코드 생성
    invitation, raw_token = auth_service.create_invitation_record(
        db, auth.org_id, req.email, auth.user_id, req.role,
        actor_role=auth.role,
    )

    # 이메일 발송
    org = org_service.get_org_or_raise(db, auth.org_id)
    inviter = user_service.get_user_or_raise(db, auth.user_id)
    invite_url = auth_service.build_invite_url(raw_token, org.slug)
    auth_service.send_invitation_email(req.email, org.name, inviter.full_name, invite_url)

    return InvitationResponse.model_validate(invitation)
