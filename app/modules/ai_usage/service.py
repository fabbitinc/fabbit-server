"""AI 사용량 로깅 서비스.

LLM 호출 결과의 토큰 사용량을 기록합니다.
public 스키마 전용 세션으로 기록하여 테넌트 search_path와 무관하게 동작합니다.
"""

import uuid
from datetime import datetime, timezone

from loguru import logger
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.core.exceptions import AppError
from app.modules.ai_usage.models import AiUsageLog
from app.modules.auth.constants import PLAN_LIMITS, PlanType
from app.modules.auth.models import Organization


def log_ai_usage(
    org_id: uuid.UUID,
    user_id: uuid.UUID,
    feature: str,
    model: str,
    input_tokens: int,
    output_tokens: int,
) -> None:
    """AI 사용량을 public 스키마에 기록.

    테넌트 DB 세션과 별개로 public 전용 세션을 사용하여
    search_path 간섭 없이 기록합니다.
    """
    db: Session = SessionLocal()
    try:
        log = AiUsageLog(
            org_id=org_id,
            user_id=user_id,
            feature=feature,
            model=model,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            credits_used=0,  # 크레딧 차감은 추후 과금 체계 도입 시 구현
        )
        db.add(log)
        db.commit()
    except Exception as e:
        db.rollback()
        logger.warning(
            "AI 사용량 로깅 실패: feature={feature} error={err}",
            feature=feature,
            err=e,
        )
    finally:
        db.close()


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
