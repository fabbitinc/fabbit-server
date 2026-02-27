"""이슈(Issue) / 변경 요청(ChangeRequest) API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.file.schemas import FileItem
from app.modules.issue.schemas import (
    AssignUsersRequest,
    AssignUsersResponse,
    AttachFilesRequest,
    ChangeRequestResponse,
    CommentResponse,
    CreateChangeRequestRequest,
    CreateCommentRequest,
    CreateIssueRequest,
    IssueResponse,
    LinkPartsRequest,
    LinkPartsResponse,
    UpdateCommentRequest,
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


# ── 담당자 ──


@router.post(
    "/issues/{issue_id}/assignees",
    response_model=AssignUsersResponse,
    status_code=200,
)
def assign_users(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: AssignUsersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 담당자 배치 할당.

    이미 할당된 사용자는 무시하고, 신규 할당 건수를 반환합니다.
    """
    return issue_commands.assign_users(db, auth, issue_id, user_ids=req.user_ids)


@router.delete(
    "/issues/{issue_id}/assignees",
    status_code=204,
)
def unassign_users(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: AssignUsersRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 담당자 배치 해제.

    요청된 사용자 ID에 해당하는 담당자를 해제합니다.
    """
    issue_commands.unassign_users(db, auth, issue_id, user_ids=req.user_ids)


# ── 부품 연결 ──


@router.post(
    "/issues/{issue_id}/parts",
    response_model=LinkPartsResponse,
    status_code=200,
)
def link_parts(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: LinkPartsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈에 부품 배치 연결.

    이미 연결된 부품은 무시하고, 신규 연결 건수를 반환합니다.
    각 부품의 존재 여부를 사전 검증합니다.
    """
    return issue_commands.link_parts(db, auth, issue_id, part_ids=req.part_ids)


@router.delete(
    "/issues/{issue_id}/parts",
    status_code=204,
)
def unlink_parts(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: LinkPartsRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈에서 부품 배치 해제.

    요청된 부품 ID에 해당하는 연결을 해제합니다.
    """
    issue_commands.unlink_parts(db, auth, issue_id, part_ids=req.part_ids)


# ── 댓글 ──


@router.post(
    "/issues/{issue_id}/comments",
    response_model=CommentResponse,
    status_code=201,
)
def create_comment(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: CreateCommentRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 생성.

    댓글 본문은 1~10,000자까지 입력 가능합니다.
    """
    return issue_commands.create_comment(db, auth, issue_id, body=req.body)


@router.patch(
    "/issues/{issue_id}/comments/{comment_id}",
    response_model=CommentResponse,
    status_code=200,
)
def update_comment(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    comment_id: uuid.UUID,
    req: UpdateCommentRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 수정.

    본인이 작성한 댓글만 수정할 수 있습니다.
    """
    return issue_commands.update_comment(db, auth, issue_id, comment_id, body=req.body)


@router.delete(
    "/issues/{issue_id}/comments/{comment_id}",
    status_code=204,
)
def delete_comment(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    comment_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 삭제.

    본인이 작성한 댓글만 삭제할 수 있습니다.
    """
    issue_commands.delete_comment(db, auth, issue_id, comment_id)


# ── 첨부파일 ──


@router.post(
    "/issues/{issue_id}/files",
    response_model=list[FileItem],
    status_code=200,
)
def add_files(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: AttachFilesRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈에 첨부파일 배치 연결.

    업로드 완료(`UPLOADED`) 상태이며 아직 소유자가 없는 파일만 연결 가능합니다.
    최대 20개까지 한 번에 연결할 수 있습니다.
    """
    return issue_commands.add_files(db, auth, issue_id, file_ids=req.file_ids)


@router.delete(
    "/issues/{issue_id}/files/{file_id}",
    status_code=204,
)
def delete_file(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    file_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 첨부파일 1건 삭제.

    해당 이슈에 연결된 파일만 삭제할 수 있습니다.
    파일은 소프트 삭제 처리됩니다.
    """
    issue_commands.delete_file(db, auth, issue_id, file_id)
