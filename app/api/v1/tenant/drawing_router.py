"""도면 분석 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.drawing import service
from app.modules.drawing.schemas import (
    DrawingAnalysisListResponse,
    DrawingAnalysisResponse,
    DrawingAnalyzeRequest,
    DrawingAnalyzeResponse,
    DrawingConfirmRequest,
    DrawingSynthesisJobResponse,
    DrawingSynthesisStartRequest,
)

router = APIRouter(prefix="/api/v1/drawings", tags=["drawings"])


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
