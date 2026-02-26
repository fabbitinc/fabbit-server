"""프로젝트 소속 부품 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part.models import Part
from app.modules.project import repository as repo
from app.modules.project.models import ProjectPart
from app.modules.project.schemas import ProjectPartSummary, ProjectPartsResponse


@transactional(read_only=True)
def get_project_parts(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    *,
    offset: int = 0,
    limit: int = 20,
) -> ProjectPartsResponse:
    """Project에 연결된 Part 목록 페이징 조회."""
    project = repo.get_project_by_id(db, project_id)
    if not project:
        raise AppError(
            message=f"Project '{project_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    query = (
        db.query(Part)
        .join(ProjectPart, ProjectPart.part_id == Part.id)
        .filter(ProjectPart.project_id == project_id)
    )
    total = query.count()
    parts = query.order_by(Part.part_number).offset(offset).limit(limit).all()

    items = [
        ProjectPartSummary(id=p.id, part_number=p.part_number, name=p.name)
        for p in parts
    ]
    return ProjectPartsResponse(total=total, items=items)
