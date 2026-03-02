"""Part 상세 조회."""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.file.constants import FileStatus
from app.modules.file.models import File
from app.modules.part import repository as repo
from app.modules.part.mapper import to_related_drawing
from app.modules.part.schemas import PartDetailResponse


@transactional(read_only=True)
def get_part_detail(
    db: Session, auth: AuthContext, part_id: uuid.UUID
) -> PartDetailResponse:
    # 속성: RDS
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(
            message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    # Drawing: RDS (1:1, 가벼움)
    drawing_row = repo.get_drawing(db, part.id)

    # count 쿼리
    children_count = repo.count_children(db, part.id)
    parents_count = repo.count_parents(db, part.id)
    suppliers_count = repo.count_suppliers(db, part.id)
    projects_count = repo.count_projects(db, part.id)
    assignees_count = repo.count_unique_assignees(db, part.id)

    # 파일 count: UPLOADED 상태만
    files_count = (
        db.query(func.count(File.id))
        .filter(
            File.owner_type == "part",
            File.owner_id == part.id,
            File.status == FileStatus.UPLOADED,
        )
        .scalar()
        or 0
    )

    extended = {
        k: v for k, v in (part.extended_properties or {}).items() if v is not None
    }

    return PartDetailResponse(
        id=part.id,
        part_number=part.part_number,
        name=part.name,
        revision=part.revision,
        material=part.material,
        unit=part.unit,
        description=part.description,
        category=part.category,
        lifecycle_state=part.lifecycle_state,
        is_phantom=part.is_phantom,
        lead_time_days=part.lead_time_days,
        extended_properties=extended,
        drawing=to_related_drawing(drawing_row),
        children_count=children_count,
        parents_count=parents_count,
        suppliers_count=suppliers_count,
        files_count=files_count,
        projects_count=projects_count,
        assignees_count=assignees_count,
    )
