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
            storage_gb=limits.storage_gb,
            max_bom=limits.max_bom,
            max_drawing_parses=limits.max_drawing_parses,
            max_chats=limits.max_chats,
            price_monthly=limits.price_monthly,
        )
        for pt, limits in PLAN_LIMITS.items()
    ]
