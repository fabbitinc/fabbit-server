"""프로젝트 멤버 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.user.models import User
from app.modules.project import repository as project_repo
from app.modules.project import service as project_service
from app.modules.project.schemas import ProjectMemberListResponse, ProjectMemberSummary


@transactional(read_only=True)
def list_members(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
) -> ProjectMemberListResponse:
    """프로젝트 멤버 목록 조회."""
    project_service.get_or_raise(db, project_id)
    member_ids = project_repo.list_member_ids(db, project_id)
    if not member_ids:
        return ProjectMemberListResponse(items=[])

    # User 정보 배치 조회 (cross-schema)
    users = db.query(User).filter(User.id.in_(member_ids)).all()
    user_map = {u.id: u for u in users}

    items = [
        ProjectMemberSummary(
            user_id=uid,
            full_name=user_map[uid].full_name if uid in user_map else "",
            email=user_map[uid].email if uid in user_map else "",
        )
        for uid in member_ids
    ]
    return ProjectMemberListResponse(items=items)
