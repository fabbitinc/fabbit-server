"""Part 담당자 추가/제거."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import service as part_service
from app.modules.part.schemas import ManageAssignmentsResponse


@transactional()
def add_assignees(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    assignments: list[dict],
) -> ManageAssignmentsResponse:
    """Part에 담당자 배치 추가."""
    part_service.get_or_raise(db, part_id)
    count = part_service.add_assignees(db, part_id, assignments)
    return ManageAssignmentsResponse(count=count)


@transactional()
def remove_assignees(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    assignments: list[dict],
) -> ManageAssignmentsResponse:
    """Part에서 담당자 배치 제거."""
    part_service.get_or_raise(db, part_id)
    count = part_service.remove_assignees(db, part_id, assignments)
    return ManageAssignmentsResponse(count=count)
