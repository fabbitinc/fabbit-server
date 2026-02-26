"""동기 SQLAlchemy Session용 Unit of Work."""

from __future__ import annotations

from types import TracebackType

from sqlalchemy.orm import Session

from app.core.aggregate import AggregateRoot
from app.core.domain_event import DomainEvent
from app.core.event_bus import event_bus


class UnitOfWork:
    """요청에서 주입된 Session의 트랜잭션 경계를 제어합니다.

    commit 시 AggregateRoot에서 도메인 이벤트를 수집하고,
    commit 전에 EventBus로 발행하여 핸들러가 같은 트랜잭션에서 실행됩니다.
    """

    def __init__(self, db: Session) -> None:
        self.db = db

    def __enter__(self) -> UnitOfWork:
        return self

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_val: BaseException | None,
        exc_tb: TracebackType | None,
    ) -> None:
        if exc_type is not None:
            self.rollback()

    def commit(self) -> None:
        events = self._collect_aggregate_events()
        if events:
            event_bus.publish_all(events)
        self.db.commit()

    def rollback(self) -> None:
        self.db.rollback()

    def _collect_aggregate_events(self) -> list[DomainEvent]:
        """Session에 로드된 모든 AggregateRoot에서 이벤트를 수집.

        register_event()만 호출하고 mapped attribute를 변경하지 않은 경우에도
        이벤트가 누락되지 않도록 identity_map 전체를 스캔한다.
        """
        events: list[DomainEvent] = []
        for obj in self.db.identity_map.values():
            if isinstance(obj, AggregateRoot):
                events.extend(obj.collect_events())
        return events
