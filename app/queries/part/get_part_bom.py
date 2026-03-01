"""Part BOM 직접 관계 조회 (1-depth)."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.mapper import to_bom_child, to_bom_parent
from app.modules.part.schemas import PartBomResponse


@transactional(read_only=True)
def get_part_bom(
    db: Session, auth: AuthContext, part_id: uuid.UUID
) -> PartBomResponse:
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(
            message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    children_rows = repo.get_children(db, part.id)
    parents_rows = repo.get_parents(db, part.id)

    return PartBomResponse(
        children=[to_bom_child(r) for r in children_rows],
        parents=[to_bom_parent(r) for r in parents_rows],
    )
