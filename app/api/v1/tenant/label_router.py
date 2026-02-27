"""라벨(Label) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Response
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.label.schemas import (
    CreateLabelRequest,
    LabelListResponse,
    LabelResponse,
    UpdateLabelRequest,
)
from app.queries import label as label_queries
from app.use_cases import label as label_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}", tags=["labels"])


@router.get("/labels", response_model=LabelListResponse)
def list_labels(
    project_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 라벨 목록 조회.

    프로젝트에 등록된 모든 라벨을 이름순으로 반환합니다.
    """
    return label_queries.list_labels(db, auth, project_id)


@router.post("/labels", response_model=LabelResponse, status_code=201)
def create_label(
    project_id: uuid.UUID,
    req: CreateLabelRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """라벨 생성.

    프로젝트 내에서 라벨 이름은 고유해야 합니다.
    색상은 `#RRGGBB` 형식의 hex 코드입니다.
    """
    return label_commands.create_label(
        db, auth, project_id, name=req.name, color=req.color, description=req.description
    )


@router.patch("/labels/{label_id}", response_model=LabelResponse)
def update_label(
    project_id: uuid.UUID,
    label_id: uuid.UUID,
    req: UpdateLabelRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """라벨 수정.

    변경할 필드만 요청 본문에 포함하면 됩니다.
    `description`을 `null`로 보내면 설명이 제거됩니다.
    """
    # description이 요청에 명시적으로 포함되었는지 확인
    raw = req.model_dump(exclude_unset=True)
    unset_description = "description" in raw and raw["description"] is None

    return label_commands.update_label(
        db,
        auth,
        label_id,
        name=req.name,
        description=req.description,
        color=req.color,
        _unset_description=unset_description,
    )


@router.delete("/labels/{label_id}", status_code=204)
def delete_label(
    project_id: uuid.UUID,
    label_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """라벨 삭제.

    삭제된 라벨은 복구할 수 없습니다.
    """
    label_commands.delete_label(db, auth, label_id)
    return Response(status_code=204)
