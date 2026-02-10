"""동기 SQLAlchemy Session용 Unit of Work."""

from types import TracebackType

from sqlalchemy.orm import Session


class UnitOfWork:
    """요청에서 주입된 Session의 트랜잭션 경계를 제어합니다."""

    def __init__(self, db: Session) -> None:
        self.db = db

    def __enter__(self) -> "UnitOfWork":
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
        self.db.commit()

    def rollback(self) -> None:
        self.db.rollback()
