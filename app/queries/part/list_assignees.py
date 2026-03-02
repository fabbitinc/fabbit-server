"""Part 담당자 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.part import repository as part_repo
from app.modules.part.schemas import PartAssigneeListResponse, PartAssigneeSummary
from app.modules.user.models import User


@transactional(read_only=True)
def list_assignees(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
) -> PartAssigneeListResponse:
    """Part 담당자 목록 조회 — User cross-schema 배치 조회."""
    part = part_repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message="Part를 찾을 수 없습니다", code="NOT_FOUND")

    assignees = part_repo.list_assignees(db, part_id)
    if not assignees:
        return PartAssigneeListResponse(items=[])

    # User 정보 배치 조회 (cross-schema)
    user_ids = list({a.user_id for a in assignees})
    users = db.query(User).filter(User.id.in_(user_ids)).all()
    user_map = {u.id: u for u in users}

    items = [
        PartAssigneeSummary(
            user_id=a.user_id,
            full_name=user_map[a.user_id].full_name if a.user_id in user_map else "",
            email=user_map[a.user_id].email if a.user_id in user_map else "",
            discipline=a.discipline,
        )
        for a in assignees
    ]
    return PartAssigneeListResponse(items=items)
