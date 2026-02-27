"""공통 Dependency.

인증, DB 세션 등 API 엔드포인트에서 공통으로 사용하는 의존성입니다.
"""

from collections.abc import Generator

from fastapi import Depends, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import event, text
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.config import settings
from app.core.database import SessionLocal
from app.core.exceptions import AppError
from app.modules.auth.provisioning import org_id_to_schema

# Swagger UI에 Authorize 버튼 표시 (실제 검증은 AuthMiddleware에서 처리)
bearer_scheme = HTTPBearer(auto_error=False)


def get_db() -> Generator[Session, None, None]:
    """SQLAlchemy 세션 의존성 (요청 단위 생성/종료)

    테넌트 격리가 불필요한 엔드포인트에서 사용합니다.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def require_auth(
    request: Request,
    _credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> AuthContext:
    """인증 필수 의존성 — request.state.auth_context에서 읽음.

    인증 미들웨어가 JWT를 검증하고 request.state에 저장한 후,
    이 의존성이 보호 엔드포인트에서 AuthContext를 추출합니다.
    _credentials 파라미터는 Swagger UI에 자물쇠 아이콘을 표시하기 위한 용도입니다.
    """
    ctx: AuthContext | None = getattr(request.state, "auth_context", None)
    if ctx is None:
        raise AppError(message="인증이 필요합니다", code="UNAUTHENTICATED")
    return ctx


def get_origin_slug(request: Request) -> str | None:
    """Origin 헤더에서 서브도메인 slug를 추출한다.

    Origin: http://test-org.lvh.me:5173 → "test-org"
    Origin: http://lvh.me:5173          → None
    """
    origin = request.headers.get("origin", "")
    if not origin:
        return None
    # "http://test-org.lvh.me:5173" → "test-org.lvh.me"
    host = origin.split("://", 1)[-1].split(":")[0]
    base = settings.base_domain
    if host == base:
        return None
    if host.endswith(f".{base}"):
        return host.removesuffix(f".{base}")
    return None


def get_tenant_db(
    auth: AuthContext = Depends(require_auth),
) -> Generator[Session, None, None]:
    """테넌트 격리 세션 의존성 (인증 기반 search_path 전환)

    SQLAlchemy 2.0에서 session.commit() 후 DBAPI 커넥션이 풀로 반환되며,
    이후 쿼리 시 새 커넥션을 받을 수 있습니다. 새 커넥션에는 테넌트 search_path가
    설정되어 있지 않으므로, after_begin 이벤트로 매 트랜잭션 시작 시
    search_path를 재설정하여 테넌트 격리를 보장합니다.
    """
    schema = org_id_to_schema(auth.org_id)
    db = SessionLocal()
    db.info["user_id"] = auth.user_id

    @event.listens_for(db, "after_begin")
    def _restore_search_path(session, transaction, connection):
        connection.execute(text(f"SET search_path = {schema}, ag_catalog, public"))

    try:
        yield db
    finally:
        db.close()
