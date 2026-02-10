"""AI 사용량 로깅 서비스.

LLM 호출 결과의 토큰 사용량을 기록합니다.
public 스키마 전용 세션으로 기록하여 테넌트 search_path와 무관하게 동작합니다.
"""

import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.modules.ai_usage.models import AiUsageLog


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
