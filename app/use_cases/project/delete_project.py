"""프로젝트 소프트 삭제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import service as project_service


@transactional()
def delete_project(
    db: Session, auth: AuthContext, project_id: uuid.UUID
) -> None:
    """프로젝트 소프트 삭제 — ADMIN 권한 필요."""
    project_service.ensure_project_admin(db, project_id, auth.user_id)
    project = project_service.get_or_raise(db, project_id)
    project_service.delete_project(db, project)
