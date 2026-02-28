"""조직 초대 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.auth import repository as repo
from app.modules.auth.schemas import InvitationListResponse, InvitationResponse


@transactional(read_only=True)
def list_invitations(
    db: Session,
    auth: AuthContext,
) -> InvitationListResponse:
    """조직의 초대 목록 조회 (최신순).

    RBAC(ADMIN 검증)은 router Depends(require_admin)에서 처리.
    """
    invitations = repo.list_invitations_by_org(db, auth.org_id)
    return InvitationListResponse(
        invitations=[
            InvitationResponse.model_validate(inv) for inv in invitations
        ]
    )
