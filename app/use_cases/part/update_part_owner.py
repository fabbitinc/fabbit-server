"""Part 담당자/팀 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.part import service as part_service
from app.modules.part.schemas import PartOwnerResponse
from app.modules.part.service import _SENTINEL
from app.modules.team.models import Team
from app.modules.user.models import User
from app.modules.user.schemas import UserSummary


@transactional()
def update_part_owner(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    owner_id: uuid.UUID | None | object = _SENTINEL,
    owner_team_id: uuid.UUID | None | object = _SENTINEL,
) -> PartOwnerResponse:
    """Part 담당자/팀 수정 (PATCH 시맨틱)."""
    part = part_service.update_owner(
        db, part_id, owner_id=owner_id, owner_team_id=owner_team_id
    )

    # 응답 변환
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
