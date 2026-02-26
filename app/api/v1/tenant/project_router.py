"""프로젝트(Project) 조회 API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.project.schemas import ProjectDetailResponse, ProjectListResponse
from app.queries import project as project_queries

router = APIRouter(prefix="/api/v1/projects", tags=["projects"])


@router.get("", response_model=ProjectListResponse)
def list_projects(
    search: str | None = Query(None, description="프로젝트명 검색"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 목록 조회.

    name으로 ILIKE 검색을 지원합니다.
    """
    return project_queries.list_projects(db, auth, search=search, offset=offset, limit=limit)


@router.get("/{project_id}", response_model=ProjectDetailResponse)
def get_project(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 상세 조회."""
    return project_queries.get_project_detail(db, auth, project_id)
