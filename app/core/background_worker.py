"""Background task 동시성 제한 — Semaphore 기반.

BackgroundTasks.add_task를 그대로 사용하면서,
세마포어로 동시 실행 수만 제한하여 커넥션 풀 고갈을 방지합니다.
"""

import threading
from typing import Callable

from app.core.config import settings

_semaphore = threading.Semaphore(settings.background_max_workers)


def guarded(fn: Callable) -> Callable:
    """세마포어로 동시 실행 수를 제한하는 래퍼."""

    def wrapper(*args, **kwargs):
        with _semaphore:
            return fn(*args, **kwargs)

    return wrapper
