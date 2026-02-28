"""조직 멤버 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_tenant_db, require_admin, require_auth
from app.core.auth_context import AuthContext
from app.modules.project.schemas import MemberListResponse
from app.queries import organization as org_queries
from app.use_cases import organization as org_commands

router = APIRouter(prefix="/api/v1/organizations/members", tags=["organizations"])


@router.get("", response_model=MemberListResponse)
def list_org_members(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """조직 멤버 목록 조회.

    현재 인증된 사용자가 속한 조직의 전체 멤버 목록을 반환합니다.
    """
    return org_queries.list_org_members(db, auth)


@router.delete("/{user_id}", status_code=204)
def remove_member(
    user_id: uuid.UUID,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_admin),
):
    """조직에서 멤버 제거.

    관리자(ADMIN)만 제거할 수 있습니다.
    조직 소유자와 자기 자신은 제거할 수 없습니다.
    """
    org_commands.remove_member(db, auth, user_id)
