"""프로젝트 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import service as project_service
from app.modules.project.models import Project


@transactional()
def update_project(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    name: str | None = None,
    description: str | None = None,
) -> Project:
    """프로젝트 정보 수정 — 변경된 필드만 반영."""
    project = project_service.get_or_raise(db, project_id)
    project_service.ensure_project_active(project)
    project_service.update_project(db, project, name=name, description=description)
    return project
