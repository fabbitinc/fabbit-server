"""이슈 타임라인 조회 — 댓글 + 활동 이력 시간순 merge."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.activity import mapper as activity_mapper
from app.modules.activity import repository as activity_repo
from app.modules.activity.constants import TargetType
from app.modules.activity.schemas import TimelineResponse
from app.modules.issue import repository as issue_repo


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

    return TimelineResponse(items=merged)
