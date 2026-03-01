"""프로젝트 보관 / 보관 해제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.project import service as project_service


@transactional()
def archive_project(
    db: Session, auth: AuthContext, project_id: uuid.UUID
) -> None:
    """프로젝트 보관 — ADMIN 권한 필요, 이미 보관된 경우 에러."""
    project_service.ensure_project_admin(db, project_id, auth.user_id)
    project = project_service.get_or_raise(db, project_id)
    project_service.ensure_project_active(project)
    project_service.archive_project(db, project)


@transactional()
def unarchive_project(
    db: Session, auth: AuthContext, project_id: uuid.UUID
) -> None:
    """프로젝트 보관 해제 — ADMIN 권한 필요, 보관 상태가 아니면 에러."""
    project_service.ensure_project_admin(db, project_id, auth.user_id)
    project = project_service.get_or_raise(db, project_id)
    if not project.is_archived:
        raise AppError(
            message="보관 상태가 아닌 프로젝트는 복원할 수 없습니다",
            code="BAD_REQUEST",
        )
    project_service.unarchive_project(db, project)
