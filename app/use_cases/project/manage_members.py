"""프로젝트 멤버 추가/제거."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import service as project_service
from app.modules.project.schemas import ManageMembersResponse


@transactional()
def add_members(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    user_ids: list[uuid.UUID],
    role: str = "MEMBER",
) -> ManageMembersResponse:
    """프로젝트에 멤버 배치 추가."""
    project_service.get_or_raise(db, project_id)
    count = project_service.add_members(db, project_id, user_ids, role=role)
    return ManageMembersResponse(count=count)


@transactional()
def remove_members(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    user_ids: list[uuid.UUID],
) -> ManageMembersResponse:
    """프로젝트에서 멤버 배치 제거."""
    project_service.get_or_raise(db, project_id)
    count = project_service.remove_members(db, project_id, user_ids)
    return ManageMembersResponse(count=count)
