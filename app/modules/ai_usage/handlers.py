"""AI 사용량 이벤트 핸들러.

AiUsageLogged 이벤트를 구독하여 public 스키마에 사용량을 기록한다.
자체 SessionLocal 세션을 사용하여 테넌트 트랜잭션과 독립적으로 동작.
"""

from loguru import logger
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.core.event_bus import event_bus
from app.modules.ai_usage.events import AiUsageLogged
from app.modules.ai_usage.models import AiUsageLog


def _on_ai_usage_logged(event: AiUsageLogged) -> None:
    """AI 사용량을 public 스키마에 기록."""
    db: Session = SessionLocal()
    try:
        log = AiUsageLog(
            org_id=event.org_id,
            user_id=event.user_id,
            feature=event.feature,
            model=event.model,
            input_tokens=event.input_tokens,
            output_tokens=event.output_tokens,
            credits_used=0,  # 크레딧 차감은 추후 과금 체계 도입 시 구현
        )
        db.add(log)
        db.commit()
    except Exception as e:
        db.rollback()
        logger.warning(
            "AI 사용량 로깅 실패: feature={feature} error={err}",
            feature=event.feature,
            err=e,
        )
    finally:
        db.close()


event_bus.subscribe(AiUsageLogged, _on_ai_usage_logged)
