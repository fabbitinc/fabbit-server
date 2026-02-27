"""이슈(Issue) / 변경 요청(ChangeRequest) API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.issue.schemas import (
    ChangeRequestResponse,
    CreateChangeRequestRequest,
    CreateIssueRequest,
    IssueResponse,
)
from app.use_cases import issue as issue_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}", tags=["issues"])


@router.post("/issues", response_model=IssueResponse, status_code=201)
def create_issue(
    project_id: uuid.UUID,
    req: CreateIssueRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 생성.

    프로젝트 내 고유 번호(`number`)가 자동 부여됩니다.
    """
    return issue_commands.create_issue(
        db, auth, project_id, title=req.title, body=req.body
    )


@router.post("/change-requests", response_model=ChangeRequestResponse, status_code=201)
def create_change_request(
    project_id: uuid.UUID,
    req: CreateChangeRequestRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청(Change Request) 생성.

    이슈 테이블에 `type=CHANGE_REQUEST`로 기록되며,
    변경 요청 고유 상태(`cr_state`)는 **DRAFT**로 시작합니다.
    """
    return issue_commands.create_change_request(
        db, auth, project_id, title=req.title, body=req.body
    )
