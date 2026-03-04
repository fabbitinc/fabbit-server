"""앱 예외 정의 및 글로벌 예외 핸들러."""

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from loguru import logger

from app.core.config import settings


class AppError(Exception):
    """애플리케이션 기본 예외. 비즈니스 규칙 위반을 표현한다."""

    def __init__(self, message: str, code: str) -> None:
        self.message = message
        self.code = code
        super().__init__(message)


# 예외 code → HTTP 상태 코드 매핑
_STATUS_MAP: dict[str, int] = {
    # 인증/인가
    "UNAUTHENTICATED": 401,
    "INVALID_CREDENTIALS": 401,
    "TOKEN_EXPIRED": 401,
    "TOKEN_INVALID": 401,
    "FORBIDDEN": 403,
    # 리소스
    "NOT_FOUND": 404,
    "ALREADY_EXISTS": 409,
    # 입력 검증
    "VALIDATION_ERROR": 422,
    # 사용량 한도 초과
    "QUOTA_EXCEEDED": 429,
    "MEMBER_LIMIT_EXCEEDED": 429,
    # 구독
    "SUBSCRIPTION_NOT_FOUND": 402,
}

_DEFAULT_STATUS = 400


async def _handle_app_error(_request: Request, exc: AppError) -> JSONResponse:
    status_code = _STATUS_MAP.get(exc.code, _DEFAULT_STATUS)
    return JSONResponse(
        status_code=status_code,
        content={"code": exc.code, "message": exc.message},
    )


async def _handle_validation_error(
    _request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Pydantic / FastAPI 입력 검증 에러를 앱 표준 형식으로 변환."""
    errors = exc.errors()
    # 첫 번째 에러의 메시지를 대표 메시지로 사용
    first = errors[0] if errors else {}
    message = first.get("msg", "입력값이 올바르지 않습니다")
    return JSONResponse(
        status_code=422,
        content={"code": "VALIDATION_ERROR", "message": message},
    )


async def _handle_unexpected_error(_request: Request, exc: Exception) -> JSONResponse:
    """예기치 않은 시스템 에러 처리."""
    logger.exception("예상치 못한 서버 오류", exc_type=type(exc).__name__)
    content: dict[str, str] = {
        "code": "INTERNAL_SERVER_ERROR",
        "message": "서버 내부 오류가 발생했습니다",
    }
    if settings.debug:
        content["detail"] = str(exc)
    return JSONResponse(status_code=500, content=content)


def register_exception_handlers(app: FastAPI) -> None:
    """FastAPI 앱에 예외 핸들러를 등록한다."""
    app.add_exception_handler(RequestValidationError, _handle_validation_error)
    app.add_exception_handler(AppError, _handle_app_error)
    app.add_exception_handler(Exception, _handle_unexpected_error)
