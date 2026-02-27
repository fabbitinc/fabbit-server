"""조직 초대 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.auth import repository as repo
from app.modules.auth.constants import MembershipRole
from app.modules.auth.schemas import InvitationListResponse, InvitationResponse


@transactional(read_only=True)
def list_invitations(
    db: Session,
    auth: AuthContext,
) -> InvitationListResponse:
    """조직의 초대 목록 조회 (최신순)."""
    if auth.role != MembershipRole.ADMIN:
        raise AppError(message="관리자만 조회할 수 있습니다", code="FORBIDDEN")

    invitations = repo.list_invitations_by_org(db, auth.org_id)
    return InvitationListResponse(
        invitations=[
            InvitationResponse.model_validate(inv) for inv in invitations
        ]
    )
