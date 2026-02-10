"""동기 서비스 함수용 트랜잭션 데코레이터."""

import contextvars
import functools
import inspect
from typing import Any, Callable, TypeVar, cast

from app.core.uow import UnitOfWork

F = TypeVar("F", bound=Callable[..., Any])

_active_db: contextvars.ContextVar[Any | None] = contextvars.ContextVar(
    "active_db",
    default=None,
)


def _resolve_db(
    sig: inspect.Signature, args: tuple[Any, ...], kwargs: dict[str, Any]
) -> Any:
    bound = sig.bind_partial(*args, **kwargs)
    db = bound.arguments.get("db")
    if db is None or not all(hasattr(db, attr) for attr in ("commit", "rollback")):
        raise TypeError("@transactional 함수는 'db: Session' 인자를 받아야 합니다")
    return db


def transactional(
    func: F | None = None,
    *,
    read_only: bool = False,
) -> Any:
    """함수 단위 트랜잭션 경계.

    - 동일 Session으로 중첩 호출되면 외부 트랜잭션을 재사용합니다.
    - read_only=True면 커밋하지 않습니다.
    """

    def decorator(method: F) -> F:
        sig = inspect.signature(method)

        @functools.wraps(method)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            db = _resolve_db(sig, args, kwargs)
            active_db = _active_db.get()

            if active_db is db:
                return method(*args, **kwargs)

            token = _active_db.set(db)
            try:
                with UnitOfWork(db) as uow:
                    result = method(*args, **kwargs)
                    if not read_only:
                        uow.commit()
                    return result
            finally:
                _active_db.reset(token)

        return cast(F, wrapper)

    if func is None:
        return decorator

    return decorator(func)
