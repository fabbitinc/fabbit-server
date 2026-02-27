"""조직 멤버 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.project.schemas import MemberListResponse
from app.queries import member as member_queries

router = APIRouter(prefix="/api/v1/members", tags=["members"])


@router.get("", response_model=MemberListResponse)
def list_org_members(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """조직 멤버 목록 조회.

    현재 인증된 사용자가 속한 조직의 전체 멤버 목록을 반환합니다.
    """
    return member_queries.list_org_members(db, auth)
