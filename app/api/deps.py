"""공통 Dependency.

인증, DB 세션 등 API 엔드포인트에서 공통으로 사용하는 의존성입니다.
"""

import uuid
from collections.abc import Generator

from fastapi import Depends, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import event, text
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext, CreateOrgContext
from app.core.config import settings
from app.core.database import SessionLocal
from app.core.exceptions import AppError
from app.infrastructure.token_provider import token_provider
from app.modules.issue.models import ChangeRequest, Issue
from app.modules.issue.constants import IssueType
from app.modules.organization.constants import ROLE_LEVEL, MembershipRole
from app.modules.organization.provisioning import org_id_to_schema

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


def require_role(min_role: MembershipRole):
    """역할 계층 기반 접근 제어 의존성 팩토리.

    min_role 이상의 역할만 통과시킨다.
    사용: auth: AuthContext = Depends(require_role(MembershipRole.ADMIN))
    """
    min_level = ROLE_LEVEL[min_role]

    def _check(auth: AuthContext = Depends(require_auth)) -> AuthContext:
        actor_level = ROLE_LEVEL.get(MembershipRole(auth.role), 0)
        if actor_level < min_level:
            raise AppError(
                message=f"{min_role.value} 이상 권한이 필요합니다", code="FORBIDDEN"
            )
        return auth
    return _check


require_admin = require_role(MembershipRole.ADMIN)
"""ADMIN 이상 역할 필수 의존성. 사용: auth: AuthContext = Depends(require_admin)"""


# TODO: DB 기반 Permission RBAC 도입 시 활성화
# def require_permission(permission: str):
#     """세분화된 권한 기반 접근 제어 의존성 팩토리.
#
#     DB에서 역할-권한 매핑을 조회하여 검증한다.
#     사용: auth: AuthContext = Depends(require_permission("members.remove"))
#     """
#     def _check(
#         auth: AuthContext = Depends(require_auth),
#         db: Session = Depends(get_db),
#     ) -> AuthContext:
#         # permissions = permission_repo.get_role_permissions(db, auth.org_id, auth.role)
#         # if permission not in permissions:
#         #     raise AppError(message="권한이 없습니다", code="FORBIDDEN")
#         return auth
#     return _check


def require_create_org_token(
    _credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    request: Request = None,
) -> CreateOrgContext:
    """조직 생성 전용 스코프 토큰 검증 의존성.

    Bearer 토큰에서 type=SCOPED, scope=create_org 검증 후 CreateOrgContext 반환.
    """
    import uuid

    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith("Bearer "):
        raise AppError(message="인증이 필요합니다", code="UNAUTHENTICATED")

    raw_token = auth_header[7:]
    payload = token_provider.decode(raw_token)

    if payload.token_type != "SCOPED" or payload.scope != "create_org":
        raise AppError(message="조직 생성 권한이 없는 토큰입니다", code="FORBIDDEN")

    return CreateOrgContext(
        user_id=uuid.UUID(payload.sub),
        email=payload.email,
    )


def get_origin_slug(request: Request) -> str | None:
    """Origin 헤더에서 서브도메인 slug를 추출한다.

    Origin: http://test-org.lvh.me:5173 → "test-org"
    Origin: http://lvh.me:5173          → None
    """
    origin = request.headers.get("origin", "")
    if not origin:
        return None
    host = origin.split("://", 1)[-1].split(":")[0]
    base = settings.base_domain
    if host == base or host == f"www.{base}":
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


def guard_archived_project(
    request: Request,
    project_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    """쓰기 요청 시 보관된 프로젝트 차단."""
    if request.method in ("GET", "HEAD", "OPTIONS"):
        return
    from app.modules.project.models import Project

    project = db.query(Project).filter(Project.id == project_id).first()
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")
    if project.is_archived:
        raise AppError(
            message="보관된 프로젝트는 수정할 수 없습니다",
            code="PROJECT_ARCHIVED",
        )


def resolve_issue(
    issue_number: int,
    db: Session = Depends(get_tenant_db),
) -> Issue:
    """이슈 번호로 Issue를 resolve하는 의존성."""
    issue = (
        db.query(Issue)
        .filter(Issue.number == issue_number, Issue.type == IssueType.ISSUE)
        .first()
    )
    if issue is None:
        raise AppError(message="이슈를 찾을 수 없습니다", code="NOT_FOUND")
    return issue


def resolve_change_request(
    issue_number: int,
    db: Session = Depends(get_tenant_db),
) -> ChangeRequest:
    """이슈 번호로 ChangeRequest를 resolve하는 의존성."""
    cr = (
        db.query(ChangeRequest)
        .filter(ChangeRequest.number == issue_number)
        .first()
    )
    if cr is None:
        raise AppError(message="변경 요청을 찾을 수 없습니다", code="NOT_FOUND")
    return cr
