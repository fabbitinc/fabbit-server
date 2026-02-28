"""활성화 및 탐색 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.activation.schemas import (
    HealthCheckResponse,
    QueryRequest,
    QueryResponse,
    StartersResponse,
)
from app.queries import activation as activation_queries
from app.use_cases import activation as activation_commands

router = APIRouter(prefix="/api/v1/activation", tags=["activation"])


@router.post("/health-check", response_model=HealthCheckResponse)
def health_check(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """그래프 데이터 상태를 점검합니다."""
    return activation_commands.health_check(db, auth)


@router.post("/query", response_model=QueryResponse)
def query_graph(
    req: QueryRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """자연어 질문을 그래프/SQL 질의로 실행합니다."""
    return activation_commands.query_graph(db, auth, req.question)


# TODO 프론트에서 처리해도됨, 관련 내용 전부삭제
@router.get("/starters", response_model=StartersResponse)
def get_starters(
    _auth: AuthContext = Depends(require_auth),
):
    """초기 탐색용 추천 질문 목록을 반환합니다."""
    return activation_queries.get_starters()
