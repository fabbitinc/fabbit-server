"""합성 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.synthesis import service
from app.modules.synthesis.schemas import (
    SynthesisJobResponse,
    SynthesisListResponse,
    SynthesisStartRequest,
)

router = APIRouter(prefix="/api/v1/synthesis", tags=["synthesis"])


@router.post("", response_model=SynthesisJobResponse)
def start_synthesis(
    req: SynthesisStartRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.start_synthesis(db, auth, req, background_tasks.add_task)


@router.get("/{job_id}", response_model=SynthesisJobResponse)
def get_synthesis_job(
    job_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    return service.get_synthesis_job(db, job_id)


@router.get("", response_model=SynthesisListResponse)
def list_synthesis_jobs(
    db: Session = Depends(get_tenant_db),
):
    return service.list_synthesis_jobs(db)
