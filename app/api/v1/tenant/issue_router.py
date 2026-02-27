"""이슈(Issue) / 변경 요청(ChangeRequest) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.activity.schemas import TimelineResponse
from app.modules.file.schemas import FileItem
from app.modules.issue.schemas import (
    AssignUsersRequest,
    AssignUsersResponse,
    AttachFilesRequest,
    ChangeRequestListResponse,
    ChangeRequestResponse,
    CommentResponse,
    CreateChangeRequestRequest,
    CreateCommentRequest,
    CreateIssueRequest,
    IssueListResponse,
    IssueResponse,
    LinkIssuesRequest,
    LinkIssuesResponse,
    LinkPartsRequest,
    LinkPartsResponse,
    UpdateCommentRequest,
)
from app.queries import issue as issue_queries
from app.use_cases import issue as issue_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}", tags=["issues"])


@router.get("/issues", response_model=IssueListResponse)
def list_issues(
    project_id: uuid.UUID,
    search: str | None = Query(None, description="제목 검색 (ILIKE)"),
    state: str | None = Query(None, description="상태 필터 (OPEN|CLOSED)"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 내 이슈 목록 조회.

    변경 요청(CR)은 제외하고 일반 이슈만 반환합니다.
    `state` 필터로 열린/닫힌 이슈를 구분할 수 있습니다.
    """
    return issue_queries.list_issues(
        db, auth, project_id, state=state, search=search, offset=offset, limit=limit
    )


@router.get("/change-requests", response_model=ChangeRequestListResponse)
def list_change_requests(
    project_id: uuid.UUID,
    search: str | None = Query(None, description="제목 검색 (ILIKE)"),
    state: str | None = Query(None, description="이슈 상태 필터 (OPEN|CLOSED)"),
    cr_state: str | None = Query(None, description="CR 상태 필터 (DRAFT|OPEN|MERGED|CLOSED)"),
    offset: int = Query(0, ge=0, description="시작 위치"),
    limit: int = Query(20, ge=1, le=100, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 내 변경 요청(CR) 목록 조회.

    `state`로 이슈 상태, `cr_state`로 CR 고유 상태를 필터링할 수 있습니다.
    """
    return issue_queries.list_change_requests(
        db,
        auth,
        project_id,
        state=state,
        cr_state=cr_state,
        search=search,
        offset=offset,
        limit=limit,
    )


@router.get("/issues/{issue_id}", response_model=IssueResponse)
def get_issue(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 상세 조회.

    라벨, 담당자, 댓글 수, 작성자 이름 등 상세 정보를 포함합니다.
    """
    return issue_queries.get_issue(db, auth, issue_id)


@router.get("/change-requests/{issue_id}", response_model=ChangeRequestResponse)
def get_change_request(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청(CR) 상세 조회.

    라벨, 담당자, 댓글 수, 작성자 이름 등 상세 정보를 포함합니다.
    """
    return issue_queries.get_change_request(db, auth, issue_id)


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


# ── 이슈 상태 전이 ──


@router.post(
    "/issues/{issue_id}/close",
    response_model=IssueResponse,
    status_code=200,
)
def close_issue(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 닫기.

    이슈 상태를 **OPEN → CLOSED**로 전환합니다.
    `closed_at` 타임스탬프가 자동 기록됩니다.
    """
    return issue_commands.close_issue(db, auth, issue_id)


@router.post(
    "/issues/{issue_id}/reopen",
    response_model=IssueResponse,
    status_code=200,
)
def reopen_issue(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 재개.

    닫힌 이슈 상태를 **CLOSED → OPEN**으로 전환합니다.
    `closed_at`이 초기화됩니다.
    """
    return issue_commands.reopen_issue(db, auth, issue_id)


# ── CR 상태 전이 ──


@router.post(
    "/change-requests/{issue_id}/open",
    response_model=ChangeRequestResponse,
    status_code=200,
)
def open_cr_for_review(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 검토 상태 전환.

    CR 상태를 **DRAFT → OPEN**으로 전환하여 검토를 요청합니다.
    """
    return issue_commands.open_cr_for_review(db, auth, issue_id)


@router.post(
    "/change-requests/{issue_id}/merge",
    response_model=ChangeRequestResponse,
    status_code=200,
)
def merge_cr(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 반영.

    CR 상태를 **MERGED**로 전환하고, 연결된 열린 이슈를 자동으로 닫습니다.
    `merged_at`, `merged_by`가 자동 기록됩니다.
    """
    return issue_commands.merge_cr(db, auth, issue_id)


@router.post(
    "/change-requests/{issue_id}/close",
    response_model=ChangeRequestResponse,
    status_code=200,
)
def close_cr(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 닫기.

    CR 상태를 **CLOSED**로 전환합니다.
    이슈 상태(`state`)도 함께 CLOSED로 변경됩니다.
    """
    return issue_commands.close_cr(db, auth, issue_id)


# ── CR-Issue 연결 ──


@router.post(
    "/change-requests/{issue_id}/issues",
    response_model=LinkIssuesResponse,
    status_code=200,
)
def link_issues(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: LinkIssuesRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청에 이슈 배치 연결.

    이미 연결된 이슈는 무시하고, 신규 연결 건수를 반환합니다.
    각 이슈의 존재 여부를 사전 검증합니다.
    """
    return issue_commands.link_issues(db, auth, issue_id, issue_ids=req.issue_ids)


@router.delete(
    "/change-requests/{issue_id}/issues",
    status_code=204,
)
def unlink_issues(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    req: LinkIssuesRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청에서 이슈 배치 해제.

    요청된 이슈 ID에 해당하는 연결을 해제합니다.
    """
    issue_commands.unlink_issues(db, auth, issue_id, issue_ids=req.issue_ids)


# ── 타임라인 ──


@router.get(
    "/issues/{issue_id}/timeline",
    response_model=TimelineResponse,
    status_code=200,
)
def get_timeline(
    project_id: uuid.UUID,
    issue_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 타임라인 조회.

    댓글과 활동 이력을 `created_at` 기준으로 시간순 merge하여 반환합니다.
    """
    return issue_queries.get_timeline(db, auth, issue_id)


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
