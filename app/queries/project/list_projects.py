"""프로젝트 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import repository as repo
from app.modules.project.schemas import ProjectListResponse, ProjectSummary


@transactional(read_only=True)
def list_projects(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> ProjectListResponse:
    """Project 목록 페이징 조회."""
    projects, total = repo.list_projects_paginated(
        db, search=search, offset=offset, limit=limit
    )

    items = [
        ProjectSummary(
            id=p.id,
            name=p.name,
            description=p.description,
        )
        for p in projects
    ]

    return ProjectListResponse(total=total, offset=offset, limit=limit, items=items)
