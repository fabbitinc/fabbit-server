"""Aggregate Root mixin — 도메인 이벤트 수집.

이벤트는 ContextVar 기반 리스트에 즉시 저장되어
ORM 객체가 GC되어도 유실되지 않는다.
"""

from __future__ import annotations

import contextvars

from app.core.domain_event import DomainEvent

_pending_events: contextvars.ContextVar[list[DomainEvent] | None] = contextvars.ContextVar(
    "pending_events", default=None
)


def init_event_collection() -> contextvars.Token:
    """이벤트 수집 시작 — @transactional이 호출."""
    return _pending_events.set([])


def take_collected_events() -> list[DomainEvent]:
    """수집된 이벤트를 꺼내고 리스트를 비운다 — UoW.commit()이 호출."""
    events = _pending_events.get()
    if events is None:
        return []
    result = list(events)
    events.clear()
    return result


def reset_event_collection(token: contextvars.Token) -> None:
    """이벤트 수집 종료 — @transactional이 호출."""
    _pending_events.reset(token)


class AggregateRoot:
    """SQLAlchemy 모델에 섞어 쓰는 Aggregate Root mixin.

    register_event()로 등록된 이벤트는 ContextVar 리스트에 즉시 저장되고,
    UnitOfWork.commit() 시 EventBus로 발행한다.
    """

    def register_event(self, event: DomainEvent) -> None:
        """이벤트를 ContextVar 수집 리스트에 등록."""
        events = _pending_events.get()
        if events is not None:
            events.append(event)
