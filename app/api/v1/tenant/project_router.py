"""프로젝트 트리 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.project import service
from app.modules.project.schemas import ProjectTreeResponse
from app.modules.synthesis import service as synthesis_service
from app.modules.synthesis.schemas import (
    SynthesisBatchStartRequest,
    SynthesisBatchStartResponse,
)

router = APIRouter(prefix="/api/v1/projects", tags=["projects"])


@router.get("/tree", response_model=ProjectTreeResponse)
def get_projects_tree(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.get_projects_tree(db, auth)


@router.post(
    "/{project_id}/synthesis/batch", response_model=SynthesisBatchStartResponse
)
def start_project_synthesis_batch(
    project_id: uuid.UUID,
    req: SynthesisBatchStartRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return synthesis_service.start_synthesis_batch(
        db,
        auth,
        project_id,
        req,
        background_tasks.add_task,
    )
