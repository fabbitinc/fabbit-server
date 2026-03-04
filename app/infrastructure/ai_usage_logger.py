"""AI 사용량 로깅 헬퍼.

운영 텔레메트리(토큰 사용량)를 public 스키마에 기록한다.
독립 세션으로 fire-and-forget 동작 — 실패해도 호출자에 영향 없음.
"""

from uuid import UUID

from loguru import logger
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.modules.ai_usage.models import AiUsageLog
from app.modules.organization.constants import AIUsageCategory


def log_ai_usage(
    org_id: UUID,
    user_id: UUID,
    category: AIUsageCategory,
    feature: str,
    model: str,
    input_tokens: int,
    output_tokens: int,
) -> None:
    """AI 사용량을 public 스키마에 기록."""
    db: Session = SessionLocal()
    try:
        log = AiUsageLog(
            org_id=org_id,
            user_id=user_id,
            category=category.value,
            feature=feature,
            model=model,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            credits_used=category.credit_cost,
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
