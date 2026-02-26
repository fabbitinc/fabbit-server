"""Part 상세 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.file import repository as file_repo
from app.modules.file.constants import FileStatus
from app.modules.file.mapper import to_file_item
from app.modules.part import repository as repo
from app.modules.part.mapper import (
    to_bom_child,
    to_bom_parent,
    to_related_drawing,
    to_related_supplier,
)
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

    # BOM 관계: RDS JOIN (name 포함)
    children_rows = repo.get_children(db, part.id)
    parents_rows = repo.get_parents(db, part.id)

    # Drawing/Supplier 관계: RDS
    drawing_row = repo.get_drawing(db, part.id)
    suppliers_rows = repo.get_suppliers(db, part.id)

    extended = {
        k: v for k, v in (part.extended_properties or {}).items() if v is not None
    }

    # 첨부파일: UPLOADED 상태만
    all_files = file_repo.get_files_by_owner(db, "part", part.id)

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
        children=[to_bom_child(r) for r in children_rows],
        parents=[to_bom_parent(r) for r in parents_rows],
        drawing=to_related_drawing(drawing_row),
        suppliers=[to_related_supplier(r) for r in suppliers_rows],
        files=[to_file_item(f) for f in all_files if f.status == FileStatus.UPLOADED],
    )
