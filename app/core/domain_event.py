"""도메인 이벤트 base class."""

from datetime import datetime, timezone
from uuid import UUID, uuid4

from pydantic import BaseModel, ConfigDict, Field


class DomainEvent(BaseModel):
    """불변 도메인 이벤트 — 모든 구체 이벤트의 부모 클래스.

    구체 이벤트는 도메인 모듈에서 정의한다.
    예: PartCreated(DomainEvent), BomLinkAdded(DomainEvent)
    """

    model_config = ConfigDict(frozen=True)

    event_id: UUID = Field(default_factory=uuid4)
    occurred_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
