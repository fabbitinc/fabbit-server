"""AI 사용량 서비스.

사용량 기록 비즈니스 로직을 제공합니다.
쿼타 검증은 Organization 도메인으로 이동.
"""

import uuid

from app.infrastructure.ai_usage_logger import log_ai_usage


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
