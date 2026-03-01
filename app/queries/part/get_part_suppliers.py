"""Part 공급사 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.mapper import to_related_supplier
from app.modules.part.schemas import PartSuppliersResponse


@transactional(read_only=True)
def get_part_suppliers(
    db: Session, auth: AuthContext, part_id: uuid.UUID
) -> PartSuppliersResponse:
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(
            message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    rows = repo.get_suppliers(db, part.id)

    return PartSuppliersResponse(
        total=len(rows),
        items=[to_related_supplier(r) for r in rows],
    )
