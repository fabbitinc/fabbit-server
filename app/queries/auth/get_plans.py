"""플랜 목록 조회."""

from app.modules.organization.constants import PLAN_LIMITS
from app.modules.organization.schemas import PlanResponse


def get_plans() -> list[PlanResponse]:
    """플랜 목록 및 제한값 조회."""
    return [
        PlanResponse(
            plan_type=pt.value,
            display_name=limits.display_name,
            description=limits.description,
            max_members=limits.max_members,
            storage_gb=limits.storage_gb,
            ai_credits=limits.ai_credits,
            price_monthly=limits.price_monthly,
        )
        for pt, limits in PLAN_LIMITS.items()
    ]
