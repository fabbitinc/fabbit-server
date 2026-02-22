"""도면 분석 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.drawing import service
from app.modules.drawing.schemas import (
    BulkRegisterDrawingRequest,
    BulkRegisterDrawingResponse,
    DrawingAnalysisListResponse,
    DrawingAnalysisResponse,
    DrawingAnalyzeRequest,
    DrawingAnalyzeResponse,
    DrawingConfirmRequest,
    DrawingListResponse,
    DrawingSynthesisJobResponse,
    DrawingSynthesisStartRequest,
)

router = APIRouter(prefix="/api/v1/drawings", tags=["drawings"])


@router.get("", response_model=DrawingListResponse)
def list_drawings(
    search: str | None = Query(None, description="drawing_number 또는 name 검색"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """도면 목록 조회.

    drawing_number, name으로 ILIKE 검색을 지원합니다.
    """
    return service.list_drawings(db, auth, search=search, offset=offset, limit=limit)


@router.post("/bulk", response_model=BulkRegisterDrawingResponse)
def bulk_register_drawings(
    req: BulkRegisterDrawingRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """도면 대량 등록.

    ## 업로드 흐름

    1. `POST /api/v1/files/upload/batch` — presigned URL 일괄 발급
    2. S3에 파일 업로드 (각 presigned URL PUT)
    3. `POST /api/v1/files/upload/batch/complete` — 업로드 일괄 확인
    4. **이 엔드포인트** — Drawing 일괄 생성

    각 항목에 `part_id`를 포함하면 해당 Part에 도면이 연결됩니다.
    DWG 파일은 자동으로 PDF/썸네일 변환이 트리거됩니다.
    """
    return service.bulk_register_drawings(db, auth, req, background_tasks.add_task)


@router.post("/analyze", response_model=DrawingAnalyzeResponse)
def analyze_drawing(
    req: DrawingAnalyzeRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.analyze_drawing(db, auth, req)


@router.post("/confirm", response_model=DrawingAnalysisResponse)
def confirm_analysis(
    req: DrawingConfirmRequest,
    db: Session = Depends(get_tenant_db),
):
    return service.confirm_analysis(db, req)


@router.get("/analyses", response_model=DrawingAnalysisListResponse)
def list_analyses(
    db: Session = Depends(get_tenant_db),
):
    return service.list_analyses(db)


@router.get("/analyses/{analysis_id}", response_model=DrawingAnalysisResponse)
def get_analysis(
    analysis_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    return service.get_analysis(db, analysis_id)


@router.post("/synthesis", response_model=DrawingSynthesisJobResponse)
def start_synthesis(
    req: DrawingSynthesisStartRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.start_drawing_synthesis(db, auth, req, background_tasks.add_task)


@router.get("/synthesis/{job_id}", response_model=DrawingSynthesisJobResponse)
def get_synthesis_job(
    job_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    return service.get_synthesis_job(db, job_id)
