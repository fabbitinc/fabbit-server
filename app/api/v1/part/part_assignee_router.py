"""Part 담당자 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.part.schemas import (
    ManageAssigneesRequest,
    ManageAssignmentsResponse,
    PartAssigneeListResponse,
)
from app.queries import part as part_queries
from app.use_cases import part as part_commands

router = APIRouter(
    prefix="/api/v1/parts/{part_id}/assignees",
    tags=["part-assignees"],
)


@router.get("", response_model=PartAssigneeListResponse)
def list_assignees(
    part_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당자 목록 조회.

    해당 Part에 배정된 담당자(User) 목록을 discipline 포함하여 반환합니다.
    """
    return part_queries.list_assignees(db, auth, part_id)


@router.post("", response_model=ManageAssignmentsResponse, status_code=201)
def add_assignees(
    part_id: uuid.UUID,
    req: ManageAssigneesRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당자 배치 추가.

    동일 Part에 같은 User + 다른 discipline 조합은 별도 배정으로 처리됩니다.
    이미 존재하는 (user_id, discipline) 조합은 무시됩니다 (멱등성).
    """
    assignments = [
        {"user_id": a.user_id, "discipline": a.discipline}
        for a in req.assignments
    ]
    return part_commands.add_assignees(db, auth, part_id, assignments)


@router.delete("", status_code=204)
def remove_assignees(
    part_id: uuid.UUID,
    req: ManageAssigneesRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """Part 담당자 배치 제거.

    지정된 (user_id, discipline) 조합을 일괄 삭제합니다.
    """
    assignments = [
        {"user_id": a.user_id, "discipline": a.discipline}
        for a in req.assignments
    ]
    part_commands.remove_assignees(db, auth, part_id, assignments)
