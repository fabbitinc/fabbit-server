"""카테고리별 기본 담당자/팀 설정 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.part import repository as part_repo
from app.modules.part.schemas import CategoryDefaultItem, CategoryDefaultListResponse
from app.modules.team.models import Team
from app.modules.user.models import User
from app.modules.user.schemas import UserSummary


def _to_item(row, db: Session) -> CategoryDefaultItem:
    """CategoryDefaultAssignment → CategoryDefaultItem 변환."""
    owner_summary = None
    if row.default_owner_id:
        user = db.query(User).filter(User.id == row.default_owner_id).first()
        if user:
            owner_summary = UserSummary(
                user_id=user.id,
                full_name=user.full_name,
                email=user.email,
                phone=user.phone,
                profile_image_url=get_file_url(user.profile_image_file_key),
            )

    team_name = None
    if row.default_team_id:
        team = db.query(Team).filter(Team.id == row.default_team_id).first()
        if team:
            team_name = team.name

    return CategoryDefaultItem(
        id=row.id,
        category=row.category,
        default_owner_id=row.default_owner_id,
        default_owner=owner_summary,
        default_team_id=row.default_team_id,
        default_team_name=team_name,
    )


@transactional(read_only=True)
def list_category_defaults(
    db: Session, auth: AuthContext
) -> CategoryDefaultListResponse:
    """카테고리별 기본 담당자/팀 설정 목록 조회."""
    rows = part_repo.list_category_defaults(db)
    items = [_to_item(r, db) for r in rows]
    return CategoryDefaultListResponse(items=items)
