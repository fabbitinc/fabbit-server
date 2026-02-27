"""프로젝트 활동 피드 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.activity import mapper, repository as activity_repo
from app.modules.activity.constants import TargetType
from app.modules.activity.schemas import ActivityListResponse


@transactional(read_only=True)
def get_activities(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    *,
    cursor: uuid.UUID | None = None,
    limit: int = 20,
) -> ActivityListResponse:
    """Project scope 활동 피드 cursor 기반 조회."""
    activities = activity_repo.list_by_target_cursor(
        db, TargetType.PROJECT, project_id, cursor=cursor, limit=limit
    )
    items = [mapper.to_activity_response(a) for a in activities]
    next_cursor = activities[-1].id if len(activities) == limit else None
    return ActivityListResponse(items=items, next_cursor=next_cursor)
