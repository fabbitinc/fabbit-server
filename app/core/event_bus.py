"""동기 in-process 이벤트 버스."""

from __future__ import annotations

from collections import defaultdict
from collections.abc import Callable

from loguru import logger

from app.core.domain_event import DomainEvent

# 핸들러 타입: DomainEvent를 받아 None을 반환하는 callable
EventHandler = Callable[[DomainEvent], None]


class EventBus:
    """동기 이벤트 버스 — commit 완료 후 핸들러를 순차 실행.

    핸들러 예외는 로깅 후 다음 핸들러를 계속 실행한다.
    commit 이후에 발행되므로 핸들러 실패가 비즈니스 트랜잭션을 깨뜨리지 않는다.
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
            try:
                handler(event)
            except Exception:
                logger.exception(
                    "이벤트 핸들러 실행 실패: event_type={event_type}",
                    event_type=type(event).__name__,
                )

    def publish_all(self, events: list[DomainEvent]) -> None:
        """여러 이벤트를 순차 발행."""
        for event in events:
            self.publish(event)


# 모듈 싱글턴
event_bus = EventBus()
