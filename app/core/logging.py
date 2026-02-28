"""로깅 설정 - loguru + OpenTelemetry 통합.

OTel-Native 전략:
- 모든 로그에 trace_id, span_id 자동 주입
- 비즈니스 문맥만 로깅 (OTel이 처리하는 것은 제외)
"""

import logging
import sys
from typing import TYPE_CHECKING

from loguru import logger

from app.core.config import settings

if TYPE_CHECKING:
    from loguru import Record


class InterceptHandler(logging.Handler):
    """표준 logging을 loguru로 리다이렉트하는 핸들러."""

    def emit(self, record: logging.LogRecord) -> None:
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = record.levelno

        frame, depth = logging.currentframe(), 2
        while frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back  # type: ignore
            depth += 1

        logger.opt(depth=depth, exception=record.exc_info).log(
            level, record.getMessage()
        )


def _trace_context_patcher(record: "Record") -> None:
    """로그 레코드에 OTel trace context 주입."""
    from app.core.observability import get_current_trace_context

    ctx = get_current_trace_context()
    record["extra"]["trace_id"] = ctx["trace_id"]
    record["extra"]["span_id"] = ctx["span_id"]


def _format_log_message(record: "Record") -> str:
    """로그 포맷 생성 (컬러 콘솔 출력 + trace_id 표시)."""
    trace_id = record["extra"].get("trace_id")
    trace_part = f" | <dim>{trace_id[:8]}...</dim>" if trace_id else ""

    return (
        "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | "
        "<level>{level: <8}</level> | "
        "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan>"
        f"{trace_part} | "
        "<level>{message}</level>\n"
        "{exception}"
    )


def setup_logging() -> None:
    """로깅 설정 초기화."""
    # 기본 핸들러 제거
    logger.remove()

    # 로그 레벨 설정
    log_level = settings.log_level if not settings.debug else "DEBUG"

    # 콘솔 출력 설정 (trace context 포함)
    logger.add(
        sys.stderr,
        level=log_level,
        format=_format_log_message,
        colorize=True,
        backtrace=settings.debug,
        diagnose=settings.debug,
    )

    # OTel trace context 자동 주입
    if settings.otel_enabled:
        logger.configure(patcher=_trace_context_patcher)

    # uvicorn, fastapi 등 표준 logging을 loguru로 통합
    intercept_handler = InterceptHandler()
    logging.root.handlers = [intercept_handler]
    logging.root.setLevel(log_level)

    for name in [
        "uvicorn",
        "uvicorn.error",
        "uvicorn.access",
        "fastapi",
    ]:
        log = logging.getLogger(name)
        log.handlers = []
        log.propagate = True

    # SQLAlchemy 쿼리 로그는 DEBUG에서만 (OTel이 span으로 처리)
    sql_logger = logging.getLogger("sqlalchemy.engine")
    sql_logger.setLevel(logging.WARNING)
    # sql_logger.setLevel(logging.WARNING if not settings.debug else logging.INFO)
    sql_logger.handlers = []
    sql_logger.propagate = True
