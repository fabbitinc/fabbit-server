"""대시보드 통계 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.dashboard.schemas import DashboardStatsResponse
from app.queries import dashboard as dashboard_queries

router = APIRouter(prefix="/api/v1/dashboard", tags=["dashboard"])


@router.get("/stats", response_model=DashboardStatsResponse)
def get_dashboard_stats(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """대시보드 통계 조회.

    Part 총 수, 금주 추가 수, BOM 링크 수, 최근 합성 작업 상태를 반환합니다.
    """
    return dashboard_queries.get_stats(db)
