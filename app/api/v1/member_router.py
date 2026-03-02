"""멤버(Member) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_tenant_db, require_admin, require_auth, require_role
from app.core.auth_context import AuthContext
from app.modules.organization.constants import MembershipRole
from app.modules.organization.schemas import ChangeRoleRequest
from app.modules.project.schemas import MemberListResponse, MemberLookupResponse
from app.queries import member as member_queries
from app.use_cases import member as member_commands

router = APIRouter(prefix="/api/v1/members", tags=["members"])


@router.get("/lookup", response_model=MemberLookupResponse)
def lookup_members(
    search: str | None = Query(None, description="이름 검색 (ILIKE)"),
    limit: int = Query(10, ge=1, le=50, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """조직 멤버 lookup 조회.

    picker/autocomplete UI를 위한 경량 목록 엔드포인트입니다.
    """
    return member_queries.lookup_members(db, auth, search=search, limit=limit)


@router.get("", response_model=MemberListResponse)
def list_org_members(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """조직 멤버 목록 조회.

    현재 인증된 사용자가 속한 조직의 전체 멤버 목록을 반환합니다.
    """
    return member_queries.list_org_members(db, auth)


@router.patch("/{user_id}/role", status_code=200)
def change_member_role(
    user_id: uuid.UUID,
    req: ChangeRoleRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_role(MembershipRole.OWNER)),
):
    """멤버 역할 변경.

    소유자(OWNER)만 변경할 수 있습니다.
    자기 자신의 역할은 변경할 수 없으며, 마지막 소유자를 강등할 수 없습니다.
    """
    member_commands.change_member_role(db, auth, user_id, req.role)


@router.delete("/{user_id}", status_code=204)
def remove_member(
    user_id: uuid.UUID,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_admin),
):
    """조직에서 멤버 제거.

    관리자(ADMIN 이상)만 제거할 수 있습니다.
    자기 자신과 상위 역할 멤버는 제거할 수 없습니다.
    """
    member_commands.remove_member(db, auth, user_id)
