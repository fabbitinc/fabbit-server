"""Aggregate Root mixin — 도메인 이벤트 수집."""

from __future__ import annotations

from app.core.domain_event import DomainEvent


class AggregateRoot:
    """SQLAlchemy 모델에 섞어 쓰는 Aggregate Root mixin.

    _events 리스트로 도메인 이벤트를 수집하고,
    UnitOfWork.commit() 시 EventBus로 발행한다.
    """

    def register_event(self, event: DomainEvent) -> None:
        """이벤트를 수집 리스트에 등록."""
        if not hasattr(self, "_events"):
            self._events: list[DomainEvent] = []
        self._events.append(event)

    def collect_events(self) -> list[DomainEvent]:
        """수집된 이벤트를 반환하고 리스트를 비운다."""
        events = getattr(self, "_events", [])
        self._events = []
        return events
