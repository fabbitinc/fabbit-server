"""동기 in-process 이벤트 버스."""

from __future__ import annotations

from collections import defaultdict
from collections.abc import Callable

from app.core.domain_event import DomainEvent

# 핸들러 타입: DomainEvent를 받아 None을 반환하는 callable
EventHandler = Callable[[DomainEvent], None]


class EventBus:
    """동기 이벤트 버스 — commit 전 트랜잭션 내에서 핸들러를 순차 실행.

    핸들러 예외는 그대로 전파되어 트랜잭션 롤백을 유발한다.
    실패해도 롤백하면 안 되는 핸들러(예: AiUsage)는 핸들러 내부에서 자체 try/except 처리.
    """

    def __init__(self) -> None:
        self._handlers: dict[type[DomainEvent], list[EventHandler]] = defaultdict(list)

    def subscribe(
        self, event_type: type[DomainEvent], handler: EventHandler
    ) -> None:
        """특정 이벤트 타입에 핸들러를 등록."""
        self._handlers[event_type].append(handler)

    def publish(self, event: DomainEvent) -> None:
        """이벤트를 발행하고 등록된 핸들러를 순차 실행."""
        for handler in self._handlers.get(type(event), []):
            handler(event)

    def publish_all(self, events: list[DomainEvent]) -> None:
        """여러 이벤트를 순차 발행."""
        for event in events:
            self.publish(event)


# 모듈 싱글턴
event_bus = EventBus()
