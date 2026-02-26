"""이벤트 핸들러 등록 레지스트리.

각 모듈의 handlers.py를 명시적으로 import하여 핸들러를 등록한다.
자동 탐색 없이 명시적 import로 등록 경로를 추적 가능하게 유지.
"""

from loguru import logger


def register_event_handlers() -> None:
    """모든 도메인 모듈의 이벤트 핸들러를 등록."""
    import app.modules.ai_usage.handlers  # noqa: F401
    import app.modules.file.handlers  # noqa: F401

    logger.info("이벤트 핸들러 등록 완료")
