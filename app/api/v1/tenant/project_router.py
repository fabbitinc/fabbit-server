"""프로젝트 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.project import service
from app.modules.project.schemas import (
    CreateFolderRequest,
    CreateProjectRequest,
    FolderResponse,
    MoveFolderRequest,
    ProjectPartListResponse,
    ProjectResponse,
    ProjectTreeResponse,
    UpdateFolderRequest,
    UpdateProjectRequest,
)
from app.modules.synthesis.schemas import (
    SynthesisBatchStartRequest,
    SynthesisBatchStartResponse,
)

router = APIRouter(prefix="/api/v1/projects", tags=["projects"])


# ── 정적 경로 (/{project_id}보다 먼저 등록) ──


@router.get("/tree", response_model=ProjectTreeResponse)
def get_projects_tree(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.get_projects_tree(db, auth)


@router.post("/folders", response_model=FolderResponse, status_code=201)
def create_folder(
    req: CreateFolderRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.create_folder(db, auth, req)


@router.patch("/folders/{folder_id}", response_model=FolderResponse)
def update_folder(
    folder_id: uuid.UUID,
    req: UpdateFolderRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.update_folder(db, auth, folder_id, req)


@router.patch("/folders/{folder_id}/move", response_model=FolderResponse)
def move_folder(
    folder_id: uuid.UUID,
    req: MoveFolderRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.move_folder(db, auth, folder_id, req)


@router.delete("/folders/{folder_id}", status_code=204)
def delete_folder(
    folder_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    service.delete_folder(db, auth, folder_id)


# ── Project CRUD ──


@router.post("", response_model=ProjectResponse, status_code=201)
def create_project(
    req: CreateProjectRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.create_project(db, auth, req)


@router.get("/{project_id}", response_model=ProjectResponse)
def get_project(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.get_project(db, auth, project_id)


@router.patch("/{project_id}", response_model=ProjectResponse)
def update_project(
    project_id: uuid.UUID,
    req: UpdateProjectRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.update_project(db, auth, project_id, req)


@router.delete("/{project_id}", status_code=204)
def delete_project(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    service.delete_project(db, auth, project_id)


# ── 합성 배치 ──


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
    return service.start_synthesis_batch(
        db,
        auth,
        project_id,
        req,
        background_tasks.add_task,
    )


# ── ProjectPart (프로젝트-파트 연결) ──


@router.get("/{project_id}/parts", response_model=ProjectPartListResponse)
def get_project_parts(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    return service.get_project_parts(db, auth, project_id)


@router.post("/{project_id}/parts/{part_id}", status_code=204)
def add_part_to_project(
    project_id: uuid.UUID,
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    service.add_part_to_project(db, auth, project_id, part_id)


@router.delete("/{project_id}/parts/{part_id}", status_code=204)
def remove_part_from_project(
    project_id: uuid.UUID,
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    service.remove_part_from_project(db, auth, project_id, part_id)
