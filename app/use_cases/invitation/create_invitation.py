"""초대 생성 — 검증 + 초대 레코드 생성 + 이메일 발송."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import CreateInvitationRequest, InvitationResponse
from app.modules.organization import repository as org_repo
from app.modules.user import repository as user_repo


@transactional()
def create_invitation(
    db: Session,
    auth: AuthContext,
    req: CreateInvitationRequest,
) -> InvitationResponse:
    """초대 생성 및 이메일 발송."""
    # 이미 조직 멤버인지 확인
    existing_user = user_repo.get_user_by_email(db, req.email)
    if existing_user:
        existing_membership = org_repo.get_membership(db, existing_user.id, auth.org_id)
        if existing_membership:
            raise AppError(
                message="이미 조직에 소속된 멤버입니다", code="ALREADY_EXISTS"
            )

    # 초대 레코드 생성
    invitation, raw_token = auth_service.create_invitation_record(
        db, auth.org_id, req.email, auth.user_id, req.role
    )

    # 이메일 발송
    org = org_repo.get_org_by_id(db, auth.org_id)
    inviter = user_repo.get_user_by_id(db, auth.user_id)
    invite_url = auth_service.build_invite_url(raw_token, org.slug)
    auth_service.send_invitation_email(req.email, org.name, inviter.full_name, invite_url)

    return InvitationResponse.model_validate(invitation)
