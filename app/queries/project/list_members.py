"""프로젝트 멤버 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.project import repository as project_repo
from app.modules.project.schemas import ProjectMemberListResponse, ProjectMemberSummary
from app.modules.user.models import User


@transactional(read_only=True)
def list_members(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
) -> ProjectMemberListResponse:
    """프로젝트 멤버 목록 조회."""
    project = project_repo.get_project_by_id(db, project_id)
    if not project:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")
    members = project_repo.list_members(db, project_id)
    if not members:
        return ProjectMemberListResponse(items=[])

    # User 정보 배치 조회 (cross-schema)
    member_user_ids = [m.user_id for m in members]
    users = db.query(User).filter(User.id.in_(member_user_ids)).all()
    user_map = {u.id: u for u in users}

    items = [
        ProjectMemberSummary(
            user_id=m.user_id,
            full_name=user_map[m.user_id].full_name if m.user_id in user_map else "",
            email=user_map[m.user_id].email if m.user_id in user_map else "",
            role=m.role,
        )
        for m in members
    ]
    return ProjectMemberListResponse(items=items)
