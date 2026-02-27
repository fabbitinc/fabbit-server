"""Activity 도메인 모델 → Pydantic 응답 변환."""

from app.modules.activity.models import Activity
from app.modules.activity.schemas import ActivityResponse, TimelineActivityItem
from app.modules.issue.models import IssueComment
from app.modules.activity.schemas import TimelineCommentItem


def to_activity_response(activity: Activity) -> ActivityResponse:
    """Activity 모델 → ActivityResponse 변환."""
    return ActivityResponse(
        id=activity.id,
        action=activity.action,
        actor_id=activity.actor_id,
        detail=activity.detail,
        created_at=activity.created_at,
    )


def to_timeline_activity_item(activity: Activity) -> TimelineActivityItem:
    """Activity 모델 → TimelineActivityItem 변환."""
    return TimelineActivityItem(
        id=activity.id,
        action=activity.action,
        actor_id=activity.actor_id,
        detail=activity.detail,
        created_at=activity.created_at,
    )


def to_timeline_comment_item(comment: IssueComment) -> TimelineCommentItem:
    """IssueComment 모델 → TimelineCommentItem 변환."""
    return TimelineCommentItem(
        id=comment.id,
        body=comment.body,
        author_id=comment.created_by,
        created_at=comment.created_at,
    )
