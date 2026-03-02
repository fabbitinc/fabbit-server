"""이슈 타임라인 조회 — 댓글 + 활동 이력 시간순 merge."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.activity import mapper as activity_mapper
from app.modules.activity import repository as activity_repo
from app.modules.activity.constants import Action, TargetType
from app.modules.activity.schemas import TimelineResponse
from app.modules.issue import repository as issue_repo
from app.modules.user import mapper as user_mapper
from app.modules.user import repository as user_repo

# detail에 user ID가 포함되는 액션 (added/removed Ref 필드)
_USER_ID_ACTIONS = {Action.ASSIGNEE_CHANGED.value, Action.REVIEWER_CHANGED.value}


def _collect_user_ids(comment_items, activity_items, activities) -> set[uuid.UUID]:
    """타임라인 아이템에서 모든 user ID 수집."""
    ids: set[uuid.UUID] = set()

    for item in comment_items:
        if item.author_id:
            ids.add(item.author_id)

    for item in activity_items:
        ids.add(item.actor_id)

    # assignee_changed, reviewer_changed detail의 added/removed Ref에서 user ID 추출
    for a in activities:
        if a.action in _USER_ID_ACTIONS and a.detail:
            for ref in a.detail.get("added", []) + a.detail.get("removed", []):
                if isinstance(ref, dict) and ref.get("type") == "user":
                    try:
                        ids.add(uuid.UUID(ref["id"]))
                    except (ValueError, KeyError):
                        pass

    return ids


def _build_user_map(db: Session, user_ids: set[uuid.UUID]) -> dict:
    """user ID 집합 → {str(id): UserSummary} 딕셔너리."""
    if not user_ids:
        return {}
    users = user_repo.get_users_by_ids(db, list(user_ids))
    return {str(u.id): user_mapper.to_user_summary(u) for u in users}


@transactional(read_only=True)
def get_timeline(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> TimelineResponse:
    """이슈 타임라인 — 댓글 + activity를 created_at 기준 merge."""
    comments = issue_repo.list_comments_by_issue(db, issue_id)
    activities = activity_repo.list_by_target(db, TargetType.ISSUE, issue_id)

    comment_items = [activity_mapper.to_timeline_comment_item(c) for c in comments]
    activity_items = [activity_mapper.to_timeline_activity_item(a) for a in activities]

    merged = sorted(
        comment_items + activity_items,
        key=lambda item: item.created_at,
    )

    user_ids = _collect_user_ids(comment_items, activity_items, activities)
    user_map = _build_user_map(db, user_ids)

    return TimelineResponse(items=merged, users=user_map)
