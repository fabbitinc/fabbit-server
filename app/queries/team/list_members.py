"""팀 멤버 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.team import repository as team_repo
from app.modules.file.mapper import get_file_url
from app.modules.team.schemas import TeamMemberListResponse, TeamMemberSummary
from app.modules.user.models import User


@transactional(read_only=True)
def list_members(
    db: Session,
    auth: AuthContext,
    team_id: uuid.UUID,
) -> TeamMemberListResponse:
    """팀 멤버 목록 조회 — User cross-schema 배치 조회."""
    team = team_repo.get_by_id(db, team_id)
    if not team:
        raise AppError(message="팀을 찾을 수 없습니다", code="NOT_FOUND")

    members = team_repo.list_members(db, team_id)
    if not members:
        return TeamMemberListResponse(items=[])

    # User 정보 배치 조회 (cross-schema)
    member_user_ids = [m.user_id for m in members]
    users = db.query(User).filter(User.id.in_(member_user_ids)).all()
    user_map = {u.id: u for u in users}

    items = [
        TeamMemberSummary(
            user_id=m.user_id,
            full_name=user_map[m.user_id].full_name if m.user_id in user_map else "",
            email=user_map[m.user_id].email if m.user_id in user_map else "",
            phone=user_map[m.user_id].phone if m.user_id in user_map else None,
            profile_image_url=get_file_url(user_map[m.user_id].profile_image_file_key) if m.user_id in user_map else None,
        )
        for m in members
    ]
    return TeamMemberListResponse(items=items)
