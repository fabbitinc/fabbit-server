"""구독 데이터 접근."""

import uuid

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.modules.subscription.constants import SubscriptionStatus
from app.modules.subscription.models import Subscription


def get_active_subscription(db: Session, org_id: uuid.UUID) -> Subscription | None:
    """조직의 활성 구독 조회."""
    return db.scalars(
        select(Subscription).where(
            Subscription.org_id == org_id,
            Subscription.status == SubscriptionStatus.ACTIVE,
        )
    ).first()


def create_subscription(db: Session, subscription: Subscription) -> Subscription:
    """구독 행 생성."""
    db.add(subscription)
    db.flush()
    return subscription


def expire_subscription(db: Session, sub_id: uuid.UUID) -> None:
    """구독 만료 처리."""
    db.execute(
        update(Subscription)
        .where(Subscription.id == sub_id)
        .values(status=SubscriptionStatus.EXPIRED)
    )
    db.flush()
