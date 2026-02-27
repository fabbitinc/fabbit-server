from app.core.logging import setup_logging  # noqa: E402
from app.core.observability import (  # noqa: E402
    instrument_app,
    instrument_database,
    setup_telemetry,
)

# OTel → 로깅 순서 (trace context 연동)
setup_telemetry()
setup_logging()

import time  # noqa: E402
import uuid  # noqa: E402

from fastapi import FastAPI, Request  # noqa: E402
from fastapi.middleware.cors import CORSMiddleware  # noqa: E402
from loguru import logger  # noqa: E402
from starlette.middleware.base import BaseHTTPMiddleware  # noqa: E402

from app import scheduler  # noqa: E402
from app.api.v1.public.auth_router import router as auth_router  # noqa: E402
from app.api.v1.tenant.activation_router import (
    router as activation_router,  # noqa: E402
)
from app.api.v1.tenant.dashboard_router import (
    router as dashboard_router,  # noqa: E402
)
from app.api.v1.tenant.file_router import router as file_router  # noqa: E402
from app.api.v1.tenant.invitation_router import (
    router as invitation_router,  # noqa: E402
)
from app.api.v1.tenant.issue_router import router as issue_router  # noqa: E402
from app.api.v1.tenant.label_router import router as label_router  # noqa: E402
from app.api.v1.tenant.mapping_router import router as mapping_router  # noqa: E402
from app.api.v1.tenant.member_router import router as member_router  # noqa: E402
from app.api.v1.tenant.ontology_router import router as ontology_router  # noqa: E402
from app.api.v1.tenant.part_router import router as part_router  # noqa: E402
from app.api.v1.tenant.project_router import router as project_router  # noqa: E402
from app.api.v1.tenant.supplier_router import (  # noqa: E402
    router as supplier_router,
)
from app.api.v1.tenant.synthesis_router import router as synthesis_router  # noqa: E402
from app.core.auth_context import AuthContext  # noqa: E402
from app.core.config import settings  # noqa: E402
from app.core.database import (
    SessionLocal,  # noqa: E402
    engine,  # noqa: E402
)
from app.core.exceptions import register_exception_handlers  # noqa: E402
from app.infrastructure.password_hasher import hash_password  # noqa: E402
from app.infrastructure.token_provider import token_provider  # noqa: E402
from app.modules.auth import repository as auth_repo  # noqa: E402
from app.modules.auth.constants import MembershipRole  # noqa: E402
from app.modules.auth.provisioning import provision_tenant  # noqa: E402

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    debug=settings.debug,
    docs_url="/docs" if settings.debug else None,
    redoc_url="/redoc" if settings.debug else None,
    openapi_url="/openapi.json" if settings.debug else None,
)

# OTel 자동 계측
instrument_app(app)
instrument_database(engine)

# 예외 핸들러
register_exception_handlers(app)

_token_provider = token_provider

# 인증이 불필요한 경로
_PUBLIC_PATHS = frozenset(
    {
        "/health",
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/check-email",
        "/api/v1/auth/check-slug",
        "/api/v1/auth/plans",
        "/api/v1/auth/site",
        "/api/v1/auth/accept-invitation",
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
                        user_id=uuid.UUID(payload.sub),
                        email=payload.email,
                        org_id=uuid.UUID(payload.org_id),
                        role=payload.role,
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
# CORS: {slug}.lvh.me:5173 (local) / {slug}.fabbit.io (prod)
_escaped_domain = settings.base_domain.replace(".", r"\.")
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex=rf"https?://([\w-]+\.)?{_escaped_domain}(:\d+)?",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["Content-Disposition"],
)

app.include_router(activation_router)
app.include_router(auth_router)
app.include_router(dashboard_router)
app.include_router(mapping_router)
app.include_router(member_router)
app.include_router(supplier_router)
app.include_router(synthesis_router)
app.include_router(part_router)
app.include_router(project_router)
app.include_router(file_router)
app.include_router(invitation_router)
app.include_router(issue_router)
app.include_router(label_router)
app.include_router(ontology_router)


# TODO 삭제
def _bootstrap_test_account_once() -> None:
    """개발용 테스트 조직/유저를 서버 시작 시 1회 보장.

    제거가 필요하면 이 함수와 startup 이벤트 호출부를 통째로 지우면 됩니다.
    """

    # --- 간편 삭제 구간 시작: test bootstrap ---
    test_email = "test@gmail.com"
    test_password = "qwer1234"
    test_full_name = "Test User"
    test_org_slug = "test"
    test_org_name = "Test Org"
    # --- 간편 삭제 구간 끝 ---

    db = SessionLocal()
    try:
        user = auth_repo.get_user_by_email(db, test_email)
        if user is None:
            user = auth_repo.create_user(
                db,
                email=test_email,
                hashed_password=hash_password(test_password),
                full_name=test_full_name,
            )

        org = auth_repo.get_org_by_slug(db, test_org_slug)
        if org is None:
            org = auth_repo.create_organization(
                db,
                slug=test_org_slug,
                name=test_org_name,
                owner_id=user.id,
                plan_type="STARTER",
            )
            provision_tenant(db, org.id)

        memberships = auth_repo.get_user_memberships(db, user.id)
        if not any(m.org_id == org.id for m in memberships):
            auth_repo.create_membership(
                db,
                user_id=user.id,
                org_id=org.id,
                role=MembershipRole.ADMIN,
            )

        db.commit()
        logger.info(
            "테스트 계정 보장 완료: email={email} slug={slug}",
            email=test_email,
            slug=test_org_slug,
        )
    except Exception as e:
        db.rollback()
        logger.warning("테스트 계정 보장 실패: {err}", err=e)
    finally:
        db.close()


@app.on_event("startup")
def _startup_bootstrap() -> None:
    from app.core.event_registry import register_event_handlers

    register_event_handlers()
    # TODO 삭제
    _bootstrap_test_account_once()
    scheduler.start()


@app.on_event("shutdown")
def _shutdown_scheduler() -> None:
    scheduler.shutdown()


@app.get("/health")
def health():
    return {"status": "ok"}
