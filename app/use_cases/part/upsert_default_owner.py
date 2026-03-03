"""기본 담당자/팀 설정 upsert."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.part import service as part_service
from app.modules.part.schemas import PartDefaultOwnerItem
from app.modules.team.models import Team
from app.modules.user.models import User
from app.modules.user.schemas import UserSummary


def _to_item(row, db: Session) -> PartDefaultOwnerItem:
    """PartDefaultOwner → PartDefaultOwnerItem 변환."""
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

    owner_team_name = None
    if row.default_owner_team_id:
        team = db.query(Team).filter(Team.id == row.default_owner_team_id).first()
        if team:
            owner_team_name = team.name

    return PartDefaultOwnerItem(
        id=row.id,
        category=row.category,
        default_owner_id=row.default_owner_id,
        default_owner=owner_summary,
        default_owner_team_id=row.default_owner_team_id,
        default_owner_team_name=owner_team_name,
    )


@transactional()
def upsert_default_owner(
    db: Session,
    auth: AuthContext,
    category: str | None,
    owner_id: uuid.UUID | None,
    owner_team_id: uuid.UUID | None,
) -> PartDefaultOwnerItem:
    """기본 담당자/팀 설정 upsert."""
    row = part_service.upsert_default_owner(
        db,
        category=category,
        owner_id=owner_id,
        owner_team_id=owner_team_id,
    )
    return _to_item(row, db)
