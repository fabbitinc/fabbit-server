"""조직 멤버 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.organization import repository as org_repo
from app.modules.project.schemas import MemberListResponse, MemberSummary


@transactional(read_only=True)
def list_org_members(
    db: Session,
    auth: AuthContext,
) -> MemberListResponse:
    """현재 조직의 멤버 목록 조회."""
    rows = org_repo.list_org_members(db, auth.org_id)
    items = [
        MemberSummary(
            user_id=user.id,
            full_name=user.full_name,
            email=user.email,
            role=membership.role,
            job_role=membership.job_role,
            profile_image_url=get_file_url(user.profile_image_file_key),
        )
        for user, membership in rows
    ]
    return MemberListResponse(items=items)
