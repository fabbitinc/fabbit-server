"""프로젝트(Project) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.activity.schemas import ActivityListResponse
from app.modules.project.schemas import (
    CreateProjectRequest,
    LinkPartsRequest,
    LinkPartsResponse,
    ManageMembersRequest,
    ManageMembersResponse,
    ProjectDetailResponse,
    ProjectListResponse,
    ProjectMemberListResponse,
    ProjectPartsResponse,
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


@router.post("/{project_id}/parts", response_model=LinkPartsResponse)
def link_parts_to_project(
    project_id: uuid.UUID,
    req: LinkPartsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트에 부품 배치 연결.

    이미 연결된 부품은 무시되며, 신규 연결 건수를 반환합니다.
    """
    return project_commands.link_parts(db, auth, project_id, req.part_ids)


@router.delete("/{project_id}/parts", status_code=204)
def unlink_parts_from_project(
    project_id: uuid.UUID,
    req: LinkPartsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트에서 부품 배치 해제."""
    project_commands.unlink_parts(db, auth, project_id, req.part_ids)


@router.get("/{project_id}/activities", response_model=ActivityListResponse)
def get_project_activities(
    project_id: uuid.UUID,
    cursor: uuid.UUID | None = Query(None, description="이전 페이지 마지막 항목의 id"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 활동 피드 조회.

    프로젝트 범위의 활동 이력을 최신순으로 조회합니다.
    cursor에 이전 응답의 `next_cursor` 값을 전달하면 다음 페이지를 반환합니다.
    `next_cursor`가 null이면 마지막 페이지입니다.
    """
    return project_queries.get_activities(
        db, auth, project_id, cursor=cursor, limit=limit
    )


@router.get("/{project_id}/members", response_model=ProjectMemberListResponse)
def list_project_members(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 멤버 목록 조회."""
    return project_queries.list_members(db, auth, project_id)


@router.post("/{project_id}/members", response_model=ManageMembersResponse, status_code=201)
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


@router.delete("/{project_id}/members", status_code=204)
def remove_project_members(
    project_id: uuid.UUID,
    req: ManageMembersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트에서 멤버 배치 제거."""
    project_commands.remove_members(db, auth, project_id, req.user_ids)


@router.get("/{project_id}/parts", response_model=ProjectPartsResponse)
def get_project_parts(
    project_id: uuid.UUID,
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 소속 부품 목록 조회."""
    return project_queries.get_project_parts(db, auth, project_id, offset=offset, limit=limit)
