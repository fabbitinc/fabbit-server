"""AI 사용량 서비스.

BOM 쿼타 검증 및 사용량 기록 비즈니스 로직을 제공합니다.
"""

import uuid
from datetime import datetime, timezone

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.core.exceptions import AppError
from app.infrastructure.ai_usage_logger import log_ai_usage
from app.modules.ai_usage.models import AiUsageLog
from app.modules.auth.constants import PLAN_LIMITS, PlanType
from app.modules.auth.models import Organization


def check_bom_quota(org_id: uuid.UUID) -> None:
    """이번 달 BOM 분석 사용량이 플랜 한도를 초과하면 AppError 발생.

    public 스키마에서 조직 플랜 조회 + ai_usage_logs 카운트를 수행합니다.
    """
    db: Session = SessionLocal()
    try:
        org = db.get(Organization, org_id)
        if org is None:
            return

        limits = PLAN_LIMITS.get(PlanType(org.plan_type))
        if limits is None:
            return

        month_start = datetime.now(timezone.utc).replace(
            day=1, hour=0, minute=0, second=0, microsecond=0
        )
        count = (
            db.query(func.count(AiUsageLog.id))
            .filter(
                AiUsageLog.org_id == org_id,
                AiUsageLog.feature.like("mapping:%"),
                AiUsageLog.created_at >= month_start,
            )
            .scalar()
        ) or 0

        if count >= limits.max_bom:
            raise AppError(
                message=f"이번 달 BOM 분석 한도({limits.max_bom}건)를 초과했습니다. 플랜을 업그레이드해주세요.",
                code="QUOTA_EXCEEDED",
            )
    finally:
        db.close()


def log_usage(
    org_id: uuid.UUID,
    user_id: uuid.UUID,
    feature: str,
    model: str,
    input_tokens: int,
    output_tokens: int,
) -> None:
    """AI 사용량 기록 — infrastructure 헬퍼에 위임."""
    log_ai_usage(
        org_id=org_id,
        user_id=user_id,
        feature=feature,
        model=model,
        input_tokens=input_tokens,
        output_tokens=output_tokens,
    )
