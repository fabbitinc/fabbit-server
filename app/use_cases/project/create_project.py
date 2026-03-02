"""프로젝트 생성."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import service as project_service
from app.modules.project.models import Project


@transactional()
def create_project(
    db: Session,
    auth: AuthContext,
    name: str,
    description: str | None = None,
) -> Project:
    """프로젝트 생성, 생성자를 ADMIN 멤버로 등록."""
    project = project_service.create_project(
        db, name=name, description=description, owner_id=auth.user_id
    )
    return project
