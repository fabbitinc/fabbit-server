"""AI 크레딧 사용량 조회."""

import math

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.ai_usage.models import AiUsageLog
from app.modules.organization.models import Organization
from app.modules.subscription.constants import SubscriptionStatus
from app.modules.subscription.models import Subscription
from app.modules.usage.schemas import CreditCategoryItem, CreditUsageResponse


@transactional(read_only=True)
def get_credit_usage(db: Session, auth: AuthContext) -> CreditUsageResponse:
    """Organization AI 크레딧 잔여/사용량 조회."""
    org = db.query(
        Organization.plan_credits_remaining,
        Organization.bonus_credits_remaining,
    ).filter(Organization.id == auth.org_id).one()

    sub = db.query(
        Subscription.ai_credits_granted,
        Subscription.current_period_start,
        Subscription.current_period_end,
    ).filter(
        Subscription.org_id == auth.org_id,
        Subscription.status == SubscriptionStatus.ACTIVE,
    ).one()

    period_filter = [
        AiUsageLog.org_id == auth.org_id,
        AiUsageLog.created_at >= sub.current_period_start,
    ]

    total_used_raw = (
        db.query(func.coalesce(func.sum(AiUsageLog.credits_used), 0))
        .filter(*period_filter)
        .scalar()
    )
    total_used = math.ceil(total_used_raw)

    # 카테고리별 사용량 집계
    category_rows = (
        db.query(
            AiUsageLog.category,
            func.coalesce(func.sum(AiUsageLog.credits_used), 0).label("credits_used"),
            func.count().label("usage_count"),
        )
        .filter(*period_filter)
        .group_by(AiUsageLog.category)
        .all()
    )
    categories = [
        CreditCategoryItem(
            category=row.category,
            credits_used=math.ceil(row.credits_used),
            usage_count=row.usage_count,
        )
        for row in category_rows
    ]

    plan_limit = sub.ai_credits_granted
    plan_used = min(total_used, plan_limit)
    bonus_used = total_used - plan_used

    return CreditUsageResponse(
        current_period_start=sub.current_period_start,
        current_period_end=sub.current_period_end,
        total_credits_used=total_used,
        plan_credits_used=plan_used,
        plan_credits_limit=plan_limit,
        plan_credits_remaining=org.plan_credits_remaining,
        bonus_credits_used=bonus_used,
        bonus_credits_remaining=org.bonus_credits_remaining,
        categories=categories,
    )
