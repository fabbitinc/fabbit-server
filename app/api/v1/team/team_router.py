"""팀(Team) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.team.schemas import (
    CreateTeamRequest,
    TeamDetailResponse,
    TeamListResponse,
    TeamLookupResponse,
    UpdateTeamRequest,
)
from app.queries import team as team_queries
from app.use_cases import team as team_commands

router = APIRouter(prefix="/api/v1/teams", tags=["teams"])


@router.post("", response_model=TeamDetailResponse, status_code=201)
def create_team(
    req: CreateTeamRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 생성.

    팀 생성 시 생성자가 자동으로 멤버에 추가됩니다.
    """
    team = team_commands.create_team(
        db, auth, name=req.name, description=req.description
    )
    return team_queries.get_team_detail(db, auth, team.id)


@router.get("/lookup", response_model=TeamLookupResponse)
def lookup_teams(
    search: str | None = Query(None, description="팀 이름 검색 (ILIKE)"),
    limit: int = Query(10, ge=1, le=50, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 lookup 조회.

    picker/autocomplete UI를 위한 경량 목록 엔드포인트입니다.
    """
    return team_queries.lookup_teams(db, auth, search=search, limit=limit)


@router.get("", response_model=TeamListResponse)
def list_teams(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 목록 조회."""
    return team_queries.list_teams(db, auth)


@router.get("/{team_id}", response_model=TeamDetailResponse)
def get_team(
    team_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 상세 조회."""
    return team_queries.get_team_detail(db, auth, team_id)


@router.patch("/{team_id}", response_model=TeamDetailResponse)
def update_team(
    team_id: uuid.UUID,
    req: UpdateTeamRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 정보 수정.

    `name`, `description` 필드를 선택적으로 수정합니다.
    전달된 필드만 변경되며, 생략된 필드는 기존 값을 유지합니다.
    """
    team = team_commands.update_team(
        db, auth, team_id, name=req.name, description=req.description
    )
    return team_queries.get_team_detail(db, auth, team.id)


@router.delete("/{team_id}", status_code=204)
def delete_team(
    team_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """팀 삭제.

    팀 삭제 시 소속 멤버 관계도 함께 삭제됩니다.
    """
    team_commands.delete_team(db, auth, team_id)
