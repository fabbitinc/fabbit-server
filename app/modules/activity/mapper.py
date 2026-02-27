"""Activity 도메인 모델 → Pydantic 응답 변환."""

import json

from app.modules.activity.models import Activity
from app.modules.activity.schemas import ActivityResponse, TimelineActivityItem
from app.modules.issue.models import IssueComment
from app.modules.activity.schemas import TimelineCommentItem


def _parse_body(body: str | None) -> dict | None:
    """TEXT 컬럼의 body JSON 문자열을 dict로 변환."""
    if not body:
        return None
    try:
        return json.loads(body)
    except (json.JSONDecodeError, TypeError):
        return None


def _inject_action(detail: dict | None, action: str) -> dict | None:
    """detail dict에 action 키를 주입하여 union 분기에 사용."""
    if detail is None:
        return None
    return {**detail, "action": action}


def to_activity_response(activity: Activity) -> ActivityResponse:
    """Activity 모델 → ActivityResponse 변환."""
    return ActivityResponse(
        id=activity.id,
        action=activity.action,
        actor_id=activity.actor_id,
        detail=_inject_action(activity.detail, activity.action),
        created_at=activity.created_at,
    )


def to_timeline_activity_item(activity: Activity) -> TimelineActivityItem:
    """Activity 모델 → TimelineActivityItem 변환."""
    return TimelineActivityItem(
        id=activity.id,
        action=activity.action,
        actor_id=activity.actor_id,
        detail=_inject_action(activity.detail, activity.action),
        created_at=activity.created_at,
    )


def to_timeline_comment_item(comment: IssueComment) -> TimelineCommentItem:
    """IssueComment 모델 → TimelineCommentItem 변환."""
    return TimelineCommentItem(
        id=comment.id,
        body=_parse_body(comment.body),
        author_id=comment.created_by,
        created_at=comment.created_at,
    )
