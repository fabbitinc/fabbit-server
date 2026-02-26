"""Activation 그래프 헬스 체크 — LLM 미사용, 복잡한 집계 오케스트레이션."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.activation import service
from app.modules.activation.schemas import HealthCheckResponse


@transactional(read_only=True)
def health_check(db: Session, auth: AuthContext) -> HealthCheckResponse:
    """그래프/Part 데이터 상태를 점검합니다."""
    return service.health_check(db, auth)
