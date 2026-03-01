"""프로젝트 부품(Project Part) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.part.schemas import PartLookupResponse
from app.modules.project.schemas import (
    LinkPartsRequest,
    LinkPartsResponse,
    ProjectPartsResponse,
)
from app.queries import project as project_queries
from app.use_cases import project as project_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}/parts", tags=["project-parts"])


@router.get("/lookup", response_model=PartLookupResponse)
def lookup_parts(
    project_id: uuid.UUID,
    search: str | None = Query(None, description="품번 또는 품명 검색 (ILIKE)"),
    exclude_linked: bool = Query(False, description="프로젝트에 이미 연결된 부품 제외"),
    limit: int = Query(10, ge=1, le=50, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """부품 lookup 조회.

    부품 추가 picker UI를 위한 경량 목록 엔드포인트입니다.
    `exclude_linked=true`로 프로젝트에 이미 연결된 부품을 제외할 수 있습니다.
    """
    return project_queries.lookup_parts(
        db, auth, project_id, search=search, exclude_linked=exclude_linked, limit=limit
    )


@router.post("", response_model=LinkPartsResponse)
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


@router.delete("", status_code=204)
def unlink_parts_from_project(
    project_id: uuid.UUID,
    req: LinkPartsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트에서 부품 배치 해제."""
    project_commands.unlink_parts(db, auth, project_id, req.part_ids)


@router.get("", response_model=ProjectPartsResponse)
def get_project_parts(
    project_id: uuid.UUID,
    search: str | None = Query(None, description="품번 또는 품명 검색 (ILIKE)"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 소속 부품 목록 조회."""
    return project_queries.get_project_parts(
        db, auth, project_id, search=search, offset=offset, limit=limit
    )
