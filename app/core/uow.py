"""동기 SQLAlchemy Session용 Unit of Work."""

from __future__ import annotations

from types import TracebackType

from sqlalchemy.orm import Session

from app.core.aggregate import take_collected_events
from app.core.event_bus import event_bus


class UnitOfWork:
    """요청에서 주입된 Session의 트랜잭션 경계를 제어합니다.

    commit 시 ContextVar에서 도메인 이벤트를 수집하고,
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
        events = take_collected_events()
        if events:
            event_bus.publish_all(events)
        self.db.commit()

    def rollback(self) -> None:
        self.db.rollback()
