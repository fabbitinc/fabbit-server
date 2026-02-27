"""프로젝트 멤버(Project Member) API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.project.schemas import (
    ManageMembersRequest,
    ManageMembersResponse,
    ProjectMemberListResponse,
)
from app.queries import project as project_queries
from app.use_cases import project as project_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}/members", tags=["project-members"])


@router.get("", response_model=ProjectMemberListResponse)
def list_project_members(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 멤버 목록 조회."""
    return project_queries.list_members(db, auth, project_id)


@router.post("", response_model=ManageMembersResponse, status_code=201)
def add_project_members(
    project_id: uuid.UUID,
    req: ManageMembersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트에 멤버 배치 추가.

    이미 추가된 멤버는 무시되며, 신규 추가 건수를 반환합니다.
    """
    return project_commands.add_members(db, auth, project_id, req.user_ids)


@router.delete("", status_code=204)
def remove_project_members(
    project_id: uuid.UUID,
    req: ManageMembersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트에서 멤버 배치 제거."""
    project_commands.remove_members(db, auth, project_id, req.user_ids)
