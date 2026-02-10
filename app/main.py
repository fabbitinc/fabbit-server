from app.core.observability import setup_telemetry, instrument_app, instrument_database  # noqa: E402
from app.core.logging import setup_logging  # noqa: E402

# OTel → 로깅 순서 (trace context 연동)
setup_telemetry()
setup_logging()

import time  # noqa: E402

from fastapi import FastAPI, Request  # noqa: E402
from fastapi.middleware.cors import CORSMiddleware  # noqa: E402
from loguru import logger  # noqa: E402
from starlette.middleware.base import BaseHTTPMiddleware  # noqa: E402

from app.api.v1.ontology import router as ontology_router  # noqa: E402
from app.core.config import settings  # noqa: E402
from app.core.database import engine  # noqa: E402
from app.core.exceptions import register_exception_handlers  # noqa: E402

app = FastAPI(title=settings.app_name, version="0.1.0", debug=settings.debug)

# OTel 자동 계측
instrument_app(app)
instrument_database(engine)

# 예외 핸들러
register_exception_handlers(app)


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


app.add_middleware(RequestLoggingMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(ontology_router)


@app.get("/health")
def health():
    return {"status": "ok"}
