"""프로젝트 상세 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.issue import repository as issue_repo
from app.modules.project import repository as repo
from app.modules.project.schemas import ProjectDetailResponse


@transactional(read_only=True)
def get_project_detail(
    db: Session, auth: AuthContext, project_id: uuid.UUID
) -> ProjectDetailResponse:
    """Project 단건 상세 조회."""
    project = repo.get_project_by_id(db, project_id)
    if not project:
        raise AppError(
            message=f"Project '{project_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    return ProjectDetailResponse(
        id=project.id,
        name=project.name,
        description=project.description,
        part_count=repo.count_linked_parts(db, project_id),
        open_issue_count=issue_repo.count_open_issues(db, project_id),
        open_change_request_count=issue_repo.count_open_crs(db, project_id),
        created_at=project.created_at,
        updated_at=project.updated_at,
    )
