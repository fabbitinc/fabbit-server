"""부품이 속한 프로젝트 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part.models import Part
from app.modules.project.models import Project, ProjectPart
from app.modules.project.schemas import PartProjectSummary, PartProjectsResponse


@transactional(read_only=True)
def get_part_projects(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
) -> PartProjectsResponse:
    """Part가 속한 Project 목록 조회."""
    part = db.query(Part).filter(Part.id == part_id).first()
    if not part:
        raise AppError(
            message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    query = (
        db.query(Project)
        .join(ProjectPart, ProjectPart.project_id == Project.id)
        .filter(ProjectPart.part_id == part_id)
    )
    total = query.count()
    projects = query.order_by(Project.name).all()

    items = [
        PartProjectSummary(id=p.id, name=p.name, description=p.description)
        for p in projects
    ]
    return PartProjectsResponse(total=total, items=items)
