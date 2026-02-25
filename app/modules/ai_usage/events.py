"""AI 사용량 도메인 이벤트."""

from uuid import UUID

from app.core.domain_event import DomainEvent


class AiUsageLogged(DomainEvent):
    """AI 사용량 기록 요청 — fire-and-forget side-effect."""

    org_id: UUID
    user_id: UUID
    feature: str
    model: str
    input_tokens: int
    output_tokens: int
