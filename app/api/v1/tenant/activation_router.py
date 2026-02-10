"""활성화 및 탐색 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.activation import service
from app.modules.activation.schemas import (
    HealthCheckResponse,
    QueryRequest,
    QueryResponse,
    StartersResponse,
)

router = APIRouter(prefix="/api/v1/activation", tags=["activation"])


@router.post("/health-check", response_model=HealthCheckResponse)
def health_check(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.health_check(db, auth)


@router.post("/query", response_model=QueryResponse)
def query_graph(
    req: QueryRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.query_graph(db, auth, req.question)


@router.get("/starters", response_model=StartersResponse)
def get_starters(
    _auth: AuthContext = Depends(require_auth),
):
    return service.get_starters()
