"""프로젝트(Project) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.activity.constants import ActivityScope
from app.modules.activity.schemas import ActivityListResponse
from app.modules.project.schemas import (
    CreateProjectRequest,
    ProjectDetailResponse,
    ProjectListResponse,
)
from app.queries import project as project_queries
from app.use_cases import project as project_commands

router = APIRouter(prefix="/api/v1/projects", tags=["projects"])


@router.post("", response_model=ProjectDetailResponse, status_code=201)
def create_project(
    req: CreateProjectRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 생성.

    프로젝트 생성 시 기본 라벨(버그, 기능, 개선, 긴급)이 자동으로 추가됩니다.
    """
    return project_commands.create_project(
        db, auth, name=req.name, description=req.description
    )


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


@router.get("/{project_id}/activities", response_model=ActivityListResponse)
def get_project_activities(
    project_id: uuid.UUID,
    cursor: uuid.UUID | None = Query(None, description="이전 페이지 마지막 항목의 id"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    scope: list[ActivityScope] | None = Query(None, description="활동 scope 필터 (issue, cr, part, assignee, label, project)"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 활동 피드 조회.

    프로젝트 범위의 활동 이력을 최신순으로 조회합니다.
    cursor에 이전 응답의 `next_cursor` 값을 전달하면 다음 페이지를 반환합니다.
    `next_cursor`가 null이면 마지막 페이지입니다.
    scope를 지정하면 해당 도메인 영역의 활동만 필터링합니다 (복수 지정 가능).
    """
    return project_queries.get_activities(
        db, auth, project_id, cursor=cursor, limit=limit, scope=scope
    )
