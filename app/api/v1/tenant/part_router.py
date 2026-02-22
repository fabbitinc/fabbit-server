"""부품(Part) 조회 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.drawing import service as drawing_service
from app.modules.drawing.schemas import RegisterDrawingRequest, RegisterDrawingResponse
from app.modules.part import service
from app.modules.part.schemas import (
    BomTreeResponse,
    PartDetailResponse,
    PartFilterOptions,
    PartListResponse,
)

router = APIRouter(prefix="/api/v1/parts", tags=["parts"])


@router.get("/filter-options", response_model=PartFilterOptions)
def get_filter_options(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 필터 옵션 조회.

    카테고리, 수명주기 상태의 DISTINCT 값 목록을 반환합니다.
    프론트엔드에서 필터 UI의 선택지를 동적으로 구성하는 데 사용됩니다.
    """
    return service.get_filter_options(db, auth)


@router.get("", response_model=PartListResponse)
def list_parts(
    search: str | None = Query(None, description="품번 또는 품명 검색 (ILIKE)"),
    category: str | None = Query(None, description="카테고리 필터 (정확 일치)"),
    lifecycle_state: str | None = Query(None, description="수명주기 상태 필터 (정확 일치)"),
    has_drawing: bool | None = Query(None, description="도면 연결 여부 필터"),
    has_children: bool | None = Query(None, description="하위 부품 보유 여부 필터"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 목록 조회.

    각 Part에 연결된 도면번호(`drawing_number`)와 하위 부품 수(`children_count`)를 포함합니다.

    **검색**: `search` 파라미터로 품번/품명 ILIKE 검색
    **필터**: `category`, `lifecycle_state`(정확 일치), `has_drawing`, `has_children`(boolean)
    """
    return service.list_parts(
        db,
        auth,
        search=search,
        category=category,
        lifecycle_state=lifecycle_state,
        has_drawing=has_drawing,
        has_children=has_children,
        offset=offset,
        limit=limit,
    )


@router.get("/{part_id}", response_model=PartDetailResponse)
def get_part(
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 상세 조회.

    Part의 전체 속성, BOM 관계(부모/자식), 도면, 공급사 정보를 포함합니다.
    """
    return service.get_part(db, auth, part_id)


@router.get("/{part_id}/bom-tree", response_model=BomTreeResponse)
def get_part_bom_tree(
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part BOM 트리 조회.

    해당 Part를 루트로 하는 전체 BOM 계층 구조를 트리 형태로 반환합니다.
    """
    return service.get_part_bom_tree(db, auth, part_id)


@router.delete("/{part_id}/drawings", status_code=204)
def delete_drawing_from_part(
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part에 연결된 도면을 삭제합니다.

    Drawing 레코드와 연결된 파일(원본, PDF, 썸네일)이 함께 삭제됩니다.
    파일은 소프트 삭제되며, S3 파일은 보존 기간 후 배치 정리됩니다.
    """
    drawing_service.delete_drawing(db, auth, part_id)


@router.post("/{part_id}/drawings", response_model=RegisterDrawingResponse)
def register_drawing_for_part(
    part_id: uuid.UUID,
    req: RegisterDrawingRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part에 도면 등록.

    ## 업로드 흐름

    1. `POST /api/v1/files` — presigned URL 발급
    2. S3에 파일 업로드 (presigned URL PUT)
    3. `POST /api/v1/files/{file_id}/complete` — 업로드 확인
    4. **이 엔드포인트** — Drawing 생성 + Part 연결

    DWG 파일은 자동으로 PDF/썸네일 변환이 트리거됩니다.
    변환 상태는 `GET /api/v1/drawings` 목록에서 `conversion_status`로 확인 가능합니다.
    """
    return drawing_service.register_drawing(
        db, auth, req.file_id, part_id, background_tasks.add_task
    )
