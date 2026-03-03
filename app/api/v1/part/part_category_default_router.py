"""카테고리별 기본 담당자/팀 설정 API 라우터."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_admin
from app.core.auth_context import AuthContext
from app.modules.part.schemas import (
    CategoryDefaultItem,
    CategoryDefaultListResponse,
    CategoryDefaultRequest,
)
from app.queries import part as part_queries
from app.use_cases import part as part_commands

router = APIRouter(
    prefix="/api/v1/parts/category-defaults",
    tags=["part-category-defaults"],
)


@router.get("", response_model=CategoryDefaultListResponse)
def list_category_defaults(
    auth: AuthContext = Depends(require_admin),
    db: Session = Depends(get_tenant_db),
):
    """카테고리별 기본 담당자/팀 설정 목록 조회.

    모든 카테고리 기본값 설정을 반환합니다.
    category가 NULL인 항목은 fallback(전체 기본값)입니다.
    """
    return part_queries.list_category_defaults(db, auth)


@router.put("", response_model=CategoryDefaultItem)
def upsert_category_default(
    req: CategoryDefaultRequest,
    auth: AuthContext = Depends(require_admin),
    db: Session = Depends(get_tenant_db),
):
    """카테고리별 기본 담당자/팀 설정 upsert.

    - **category=null**: 전체 fallback 기본값 설정
    - **category="전자"**: 해당 카테고리 기본값 설정

    동일 category가 이미 존재하면 덮어씁니다.
    """
    return part_commands.upsert_category_default(
        db,
        auth,
        category=req.category,
        owner_id=req.default_owner_id,
        owner_team_id=req.default_owner_team_id,
    )


@router.delete("", status_code=204)
def delete_category_default(
    category: str | None = Query(None, description="삭제할 카테고리 (NULL=fallback)"),
    auth: AuthContext = Depends(require_admin),
    db: Session = Depends(get_tenant_db),
):
    """카테고리별 기본 담당자/팀 설정 삭제.

    해당 카테고리의 기본값 설정을 제거합니다.
    category 미지정 시 fallback(전체 기본값)을 삭제합니다.
    """
    part_commands.delete_category_default(db, auth, category)
