"""내 정보 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.organization import repository as org_repo
from app.modules.organization.schemas import (
    MembershipResponse,
    MeResponse,
    OrganizationResponse,
)
from app.modules.user.schemas import UserResponse


@transactional(read_only=True)
def get_me(db: Session, auth: AuthContext) -> MeResponse:
    """현재 유저 + 소속 조직 목록."""
    user = org_repo.get_user_by_id(db, auth.user_id)
    if not user:
        raise AppError(message="사용자를 찾을 수 없습니다", code="NOT_FOUND")

    memberships = org_repo.get_user_memberships(db, user.id)

    return MeResponse(
        user=UserResponse(
            id=user.id,
            email=user.email,
            full_name=user.full_name,
            phone=user.phone,
            profile_image_url=get_file_url(user.profile_image_file_key),
            is_active=user.is_active,
            created_at=user.created_at,
        ),
        memberships=[
            MembershipResponse(
                org_id=m.org_id,
                role=m.role,
                job_role=m.job_role,
                organization=OrganizationResponse(
                    id=m.organization.id,
                    slug=m.organization.slug,
                    name=m.organization.name,
                    industry=m.organization.industry,
                    team_size=m.organization.team_size,
                    plan_type=m.organization.plan_type,
                    profile_image_url=get_file_url(
                        m.organization.profile_image_file_key
                    ),
                ),
            )
            for m in memberships
        ],
    )
