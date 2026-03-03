"""Part 담당자/팀 관리 API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_admin, require_auth
from app.core.auth_context import AuthContext
from app.modules.part.schemas import (
    PartDefaultOwnerItem,
    PartDefaultOwnerListResponse,
    PartDefaultOwnerRequest,
    PartOwnerResponse,
    UpdatePartOwnerRequest,
)
from app.queries import part as part_queries
from app.use_cases import part as part_commands

router = APIRouter(prefix="/api/v1/parts", tags=["part-owner"])


# ── 개별 Part 담당자/팀 ──


@router.get("/{part_id}/owner", response_model=PartOwnerResponse)
def get_part_owner(
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당자/팀 조회.

    해당 Part에 설정된 담당자(owner)와 담당팀(owner_team) 정보를 반환합니다.
    """
    return part_queries.get_part_owner(db, auth, part_id)


@router.patch("/{part_id}/owner", response_model=PartOwnerResponse)
def update_part_owner(
    part_id: uuid.UUID,
    req: UpdatePartOwnerRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당자/팀 수정.

    PATCH 시맨틱으로 동작합니다:
    - body에 포함된 필드만 변경합니다
    - **null 값**: 해당 필드를 해제합니다 (예: `{"owner_id": null}` → 담당자 해제)
    - **UUID 값**: 해당 필드를 설정합니다 (예: `{"owner_team_id": "uuid"}` → 팀 설정)
    - **미포함 필드**: 변경하지 않습니다
    """
    kwargs = {}
    if "owner_id" in req.model_fields_set:
        kwargs["owner_id"] = req.owner_id
    if "owner_team_id" in req.model_fields_set:
        kwargs["owner_team_id"] = req.owner_team_id
    return part_commands.update_part_owner(db, auth, part_id, **kwargs)


# ── 기본 담당자/팀 (카테고리별 defaults) ──


@router.get("/owner/defaults", response_model=PartDefaultOwnerListResponse)
def list_default_owners(
    auth: AuthContext = Depends(require_admin),
    db: Session = Depends(get_tenant_db),
):
    """기본 담당자/팀 설정 목록 조회.

    모든 카테고리 기본값 설정을 반환합니다.
    category가 NULL인 항목은 fallback(전체 기본값)입니다.
    """
    return part_queries.list_default_owners(db, auth)


@router.put("/owner/defaults", response_model=PartDefaultOwnerItem)
def upsert_default_owner(
    req: PartDefaultOwnerRequest,
    auth: AuthContext = Depends(require_admin),
    db: Session = Depends(get_tenant_db),
):
    """기본 담당자/팀 설정 upsert.

    - **category=null**: 전체 fallback 기본값 설정
    - **category="전자"**: 해당 카테고리 기본값 설정

    동일 category가 이미 존재하면 덮어씁니다.
    """
    return part_commands.upsert_default_owner(
        db,
        auth,
        category=req.category,
        owner_id=req.default_owner_id,
        owner_team_id=req.default_owner_team_id,
    )


@router.delete("/owner/defaults", status_code=204)
def delete_default_owner(
    category: str | None = Query(None, description="삭제할 카테고리 (NULL=fallback)"),
    auth: AuthContext = Depends(require_admin),
    db: Session = Depends(get_tenant_db),
):
    """기본 담당자/팀 설정 삭제.

    해당 카테고리의 기본값 설정을 제거합니다.
    category 미지정 시 fallback(전체 기본값)을 삭제합니다.
    """
    part_commands.delete_default_owner(db, auth, category)
