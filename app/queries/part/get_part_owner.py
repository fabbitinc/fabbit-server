"""Part 담당자/팀 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.part import repository as part_repo
from app.modules.part.schemas import PartOwnerResponse
from app.modules.team.models import Team
from app.modules.user.models import User
from app.modules.user.schemas import UserSummary


@transactional(read_only=True)
def get_part_owner(
    db: Session, auth: AuthContext, part_id: uuid.UUID
) -> PartOwnerResponse:
    """Part 담당자/팀 조회."""
    part = part_repo.get_by_id(db, part_id)
    if not part:
        raise AppError(
            message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    # 담당자 (cross-schema)
    owner_summary = None
    if part.owner_id:
        user = db.query(User).filter(User.id == part.owner_id).first()
        if user:
            owner_summary = UserSummary(
                user_id=user.id,
                full_name=user.full_name,
                email=user.email,
                phone=user.phone,
                profile_image_url=get_file_url(user.profile_image_file_key),
            )

    # 담당팀
    owner_team_name = None
    if part.owner_team_id:
        team = db.query(Team).filter(Team.id == part.owner_team_id).first()
        if team:
            owner_team_name = team.name

    return PartOwnerResponse(
        owner_id=part.owner_id,
        owner=owner_summary,
        owner_team_id=part.owner_team_id,
        owner_team_name=owner_team_name,
    )
