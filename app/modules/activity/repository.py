"""Activity 도메인 Repository."""

import uuid

from sqlalchemy.orm import Session

from app.modules.activity.constants import TargetType
from app.modules.activity.models import Activity


def add(db: Session, entity: Activity) -> Activity:
    """Activity 저장."""
    db.add(entity)
    db.flush()
    return entity


def list_by_target(
    db: Session,
    target_type: TargetType,
    target_id: uuid.UUID,
    *,
    actions: list[str] | None = None,
) -> list[Activity]:
    """대상별 Activity 목록 조회 (시간순)."""
    query = (
        db.query(Activity)
        .filter(
            Activity.target_type == target_type,
            Activity.target_id == target_id,
        )
    )
    if actions:
        query = query.filter(Activity.action.in_(actions))
    return query.order_by(Activity.created_at).all()


def list_by_target_cursor(
    db: Session,
    target_type: TargetType,
    target_id: uuid.UUID,
    *,
    cursor: uuid.UUID | None = None,
    limit: int = 20,
    actions: list[str] | None = None,
) -> list[Activity]:
    """대상별 Activity cursor 기반 조회 (최신순).

    cursor는 이전 페이지 마지막 항목의 id.
    UUID v7이 시간순이므로 id < cursor로 이전 항목을 가져온다.
    """
    query = db.query(Activity).filter(
        Activity.target_type == target_type,
        Activity.target_id == target_id,
    )
    if cursor is not None:
        query = query.filter(Activity.id < cursor)
    if actions:
        query = query.filter(Activity.action.in_(actions))
    return query.order_by(Activity.id.desc()).limit(limit).all()
