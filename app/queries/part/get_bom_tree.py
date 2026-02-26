"""BOM 트리 조회 (정전개/역전개)."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.constants import BomDirection
from app.modules.part.schemas import BomTreeResponse
from app.queries.part._helpers import build_bom_tree


@transactional(read_only=True)
def get_bom_tree(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    direction: BomDirection = BomDirection.FORWARD,
) -> BomTreeResponse:
    """BOM 트리 조회 (정전개/역전개)."""
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    reverse = direction == BomDirection.REVERSE
    edges = repo.get_bom_edges(db, part.id, reverse=reverse)

    # 모든 part_number 수집하여 상세 필드 일괄 조회
    all_pns: set[str] = {part.part_number}
    for edge in edges:
        all_pns.add(edge["parent_pn"])
        all_pns.add(edge["child_pn"])
    parts_map = repo.bulk_get_parts(db, list(all_pns))

    root = build_bom_tree(
        root_pn=part.part_number,
        edges=edges,
        parts_map=parts_map,
    )

    return BomTreeResponse(
        root=root,
        direction=direction.value,
        total_count=len(all_pns),
    )
