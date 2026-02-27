"""프로젝트 생성 — 기본 라벨 자동 생성 포함."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import service as label_service
from app.modules.project import service as project_service
from app.modules.project.schemas import ProjectDetailResponse


@transactional()
def create_project(
    db: Session,
    auth: AuthContext,
    name: str,
    description: str | None = None,
) -> ProjectDetailResponse:
    """프로젝트 생성 및 기본 라벨 자동 생성."""
    project = project_service.create_project(db, name=name, description=description)
    label_service.seed_defaults(db, project.id)
    return ProjectDetailResponse(
        id=project.id,
        name=project.name,
        description=project.description,
        created_at=project.created_at,
        updated_at=project.updated_at,
    )
