"""사용량 조회 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.usage.schemas import CreditUsageResponse, StorageUsageResponse
from app.queries import usage as usage_queries

router = APIRouter(prefix="/api/v1/usage", tags=["usage"])


@router.get("/storage", response_model=StorageUsageResponse)
def get_storage_usage(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """스토리지 사용량 조회.

    Organization의 총 사용량/한도/초과분과 카테고리별(도면/첨부파일/기타) 내역을 반환합니다.
    """
    return usage_queries.get_storage_usage(db, auth)


@router.get("/credits", response_model=CreditUsageResponse)
def get_credit_usage(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """AI 크레딧 사용량 조회.

    플랜 크레딧 잔여, 보너스 크레딧 잔여, 총 사용량/한도를 반환합니다.
    """
    return usage_queries.get_credit_usage(db, auth)
