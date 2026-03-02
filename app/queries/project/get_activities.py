"""프로젝트 활동 피드 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.activity import mapper, repository as activity_repo
from app.modules.activity.constants import Action, TargetType, get_scope
from app.modules.activity.schemas import ActivityListResponse
from app.modules.user import mapper as user_mapper
from app.modules.user import repository as user_repo


@transactional(read_only=True)
def get_activities(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    *,
    cursor: uuid.UUID | None = None,
    limit: int = 20,
    scope: str | None = None,
    user_id: uuid.UUID | None = None,
) -> ActivityListResponse:
    """Project scope 활동 피드 cursor 기반 조회."""
    # scope → action 문자열 목록 변환
    actions: list[str] | None = None
    if scope:
        actions = [a.value for a in Action if get_scope(a.value) == scope]

    activities = activity_repo.list_by_target_cursor(
        db, TargetType.PROJECT, project_id, cursor=cursor, limit=limit, actions=actions, actor_id=user_id
    )
    items = [mapper.to_activity_response(a) for a in activities]
    next_cursor = activities[-1].id if len(activities) == limit else None

    # actor_id 수집 → 유저 정보 매핑
    actor_ids = {a.actor_id for a in activities}
    users = user_repo.get_users_by_ids(db, list(actor_ids))
    user_map = {str(u.id): user_mapper.to_user_summary(u) for u in users}

    return ActivityListResponse(items=items, next_cursor=next_cursor, users=user_map)
