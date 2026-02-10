from app.core.observability import setup_telemetry, instrument_app, instrument_database  # noqa: E402
from app.core.logging import setup_logging  # noqa: E402

# OTel → 로깅 순서 (trace context 연동)
setup_telemetry()
setup_logging()

import time  # noqa: E402
import uuid  # noqa: E402

from fastapi import FastAPI, Request  # noqa: E402
from fastapi.middleware.cors import CORSMiddleware  # noqa: E402
from loguru import logger  # noqa: E402
from starlette.middleware.base import BaseHTTPMiddleware  # noqa: E402

from app.api.v1.public.auth_router import router as auth_router  # noqa: E402
from app.api.v1.tenant.activation_router import router as activation_router  # noqa: E402
from app.api.v1.tenant.mapping_router import router as mapping_router  # noqa: E402
from app.api.v1.tenant.synthesis_router import router as synthesis_router  # noqa: E402
from app.api.v1.tenant.upload_router import router as upload_router  # noqa: E402
from app.core.auth_context import AuthContext  # noqa: E402
from app.core.config import settings  # noqa: E402
from app.core.database import engine  # noqa: E402
from app.core.exceptions import register_exception_handlers  # noqa: E402
from app.infrastructure.token_provider import TokenProvider  # noqa: E402

app = FastAPI(title=settings.app_name, version="0.1.0", debug=settings.debug)

# OTel 자동 계측
instrument_app(app)
instrument_database(engine)

# 예외 핸들러
register_exception_handlers(app)

_token_provider = TokenProvider()

# 인증이 불필요한 경로
_PUBLIC_PATHS = frozenset(
    {
        "/health",
        "/api/v1/auth/signup",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/docs",
        "/openapi.json",
        "/redoc",
    }
)


def _is_public_path(path: str) -> bool:
    """인증 미들웨어를 건너뛸 경로인지 확인."""
    return path in _PUBLIC_PATHS


# 인증 미들웨어
class AuthMiddleware(BaseHTTPMiddleware):
    """JWT → request.state.auth_context 설정.

    BaseHTTPMiddleware의 call_next는 sync 라우트를 threadpool에서 실행하므로,
    ContextVar 전파가 보장되지 않습니다. 따라서 request.state에 저장합니다.
    """

    async def dispatch(self, request: Request, call_next):
        if not _is_public_path(request.url.path):
            auth_header = request.headers.get("Authorization", "")
            if auth_header.startswith("Bearer "):
                token = auth_header[7:]
                try:
                    payload = _token_provider.decode(token)
                    request.state.auth_context = AuthContext(
                        account_id=uuid.UUID(payload.sub),
                        email=payload.email,
                        org_id=uuid.UUID(payload.org_id),
                    )
                except Exception:
                    pass  # 인증 실패 시 auth_context 미설정 → require_auth에서 401
        return await call_next(request)


# 요청 로깅 미들웨어
class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        start = time.perf_counter()
        response = await call_next(request)
        elapsed_ms = (time.perf_counter() - start) * 1000
        logger.info(
            "{method} {path} {status} {elapsed:.0f}ms",
            method=request.method,
            path=request.url.path,
            status=response.status_code,
            elapsed=elapsed_ms,
        )
        return response


app.add_middleware(AuthMiddleware)
app.add_middleware(RequestLoggingMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(activation_router)
app.include_router(auth_router)
app.include_router(mapping_router)
app.include_router(synthesis_router)
app.include_router(upload_router)


@app.get("/health")
def health():
    return {"status": "ok"}
