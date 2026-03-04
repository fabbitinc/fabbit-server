"""구독 비즈니스 로직.

빌링 이력 관리. 쿼타 검증/소비는 Organization 도메인으로 이동.
"""

import uuid

from sqlalchemy.orm import Session

from app.modules.subscription import repository as repo
from app.modules.subscription.models import Subscription


def create_initial_subscription(
    db: Session, org_id: uuid.UUID, plan_type: str
) -> Subscription:
    """조직 생성 시 초기 구독 생성 (빌링 기록 전용).

    @transactional 없음 — 호출자(service/use_case)가 트랜잭션 관리.
    """
    subscription = Subscription.create_initial(org_id=org_id, plan_type=plan_type)
    return repo.create_subscription(db, subscription)
