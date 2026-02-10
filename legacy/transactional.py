"""트랜잭션 데코레이터 - 자동 커밋/롤백 + 중첩 지원."""

import contextvars
import functools
from typing import Any, Callable, TypeVar, cast

F = TypeVar("F", bound=Callable[..., Any])

_active_uow: contextvars.ContextVar[object | None] = contextvars.ContextVar(
    "_active_uow", default=None
)


def transactional(
    func: F | None = None,
    *,
    read_only: bool = False,
) -> Any:
    """
    트랜잭션 데코레이터.

    - 중첩 호출 시 외부 트랜잭션 재사용
    - 예외 발생 시 자동 롤백 (UoW __aexit__에서 처리)

    Args:
        read_only: True면 커밋하지 않음
    """

    def decorator(method: F) -> F:
        @functools.wraps(method)
        async def wrapper(self: Any, *args: Any, **kwargs: Any) -> Any:
            if not hasattr(self, "_uow"):
                raise AttributeError(f"{type(self).__name__}에 _uow 속성이 없습니다.")

            uow = self._uow
            active = _active_uow.get()

            # 이미 활성화된 UoW면 재사용 (중첩 호출)
            if active is uow:
                return await method(self, *args, **kwargs)

            # 새로운 트랜잭션 시작
            token = _active_uow.set(uow)
            try:
                async with uow:
                    result = await method(self, *args, **kwargs)
                    if not read_only:
                        await uow.commit()
                    return result
            finally:
                _active_uow.reset(token)

        return cast(F, wrapper)

    if func is None:
        return decorator
    return decorator(func)
