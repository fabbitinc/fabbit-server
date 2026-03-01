"""변경 요청(ChangeRequest) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth, resolve_change_request
from app.core.auth_context import AuthContext
from app.modules.activity.schemas import TimelineResponse
from app.modules.file.schemas import FileItem
from app.modules.issue.models import ChangeRequest
from app.modules.issue.schemas import (
    AttachFilesRequest,
    ChangeRequestListResponse,
    ChangeRequestResponse,
    CommentResponse,
    CreateChangeRequestRequest,
    CreateCommentRequest,
    LinkIssuesRequest,
    LinkIssuesResponse,
    SyncAssigneesRequest,
    SyncAssigneesResponse,
    SyncLabelsRequest,
    SyncLabelsResponse,
    SyncPartsRequest,
    SyncPartsResponse,
    SyncReviewersRequest,
    SyncReviewersResponse,
    UpdateCommentRequest,
)
from app.queries import issue as issue_queries
from app.use_cases import issue as issue_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}/changes", tags=["changes"])


@router.get("", response_model=ChangeRequestListResponse)
def list_change_requests(
    project_id: uuid.UUID,
    search: str | None = Query(None, description="제목 검색 (ILIKE)"),
    state: str | None = Query(None, description="이슈 상태 필터 (OPEN|CLOSED)"),
    cr_state: str | None = Query(
        None, description="CR 상태 필터 (DRAFT|OPEN|MERGED|CLOSED)"
    ),
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


@router.get("/{issue_number}", response_model=ChangeRequestResponse)
def get_change_request(
    project_id: uuid.UUID,
    issue_number: int,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청(CR) 상세 조회.

    프로젝트 내 이슈 번호(`number`)로 조회합니다.
    라벨, 담당자, 댓글 수, 작성자 이름 등 상세 정보를 포함합니다.
    """
    return issue_queries.get_change_request(db, auth, project_id, issue_number)


@router.post("", response_model=ChangeRequestResponse, status_code=201)
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
    body = req.body.model_dump_json(exclude_none=True) if req.body else None
    return issue_commands.create_change_request(
        db, auth, project_id, title=req.title, body=body, issue_number=req.issue_number
    )


# ── CR 상태 전이 ──


@router.post(
    "/{issue_number}/open",
    response_model=ChangeRequestResponse,
    status_code=200,
)
def open_cr_for_review(
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 검토 상태 전환.

    CR 상태를 **DRAFT → OPEN**으로 전환하여 검토를 요청합니다.
    """
    return issue_commands.open_cr_for_review(db, auth, cr.id)


@router.post(
    "/{issue_number}/merge",
    response_model=ChangeRequestResponse,
    status_code=200,
)
def merge_cr(
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 반영.

    CR 상태를 **MERGED**로 전환하고, 연결된 열린 이슈를 자동으로 닫습니다.
    `merged_at`, `merged_by`가 자동 기록됩니다.
    """
    return issue_commands.merge_cr(db, auth, cr.id)


@router.post(
    "/{issue_number}/close",
    response_model=ChangeRequestResponse,
    status_code=200,
)
def close_cr(
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 닫기.

    CR 상태를 **CLOSED**로 전환합니다.
    이슈 상태(`state`)도 함께 CLOSED로 변경됩니다.
    """
    return issue_commands.close_cr(db, auth, cr.id)


# ── CR-Issue 연결 ──


@router.post(
    "/{issue_number}/issues",
    response_model=LinkIssuesResponse,
    status_code=200,
)
def link_issues(
    req: LinkIssuesRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청에 이슈 배치 연결.

    이미 연결된 이슈는 무시하고, 신규 연결 건수를 반환합니다.
    각 이슈의 존재 여부를 사전 검증합니다.
    """
    return issue_commands.link_issues(db, auth, cr.id, issue_ids=req.issue_ids)


@router.delete(
    "/{issue_number}/issues",
    status_code=204,
)
def unlink_issues(
    req: LinkIssuesRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청에서 이슈 배치 해제.

    요청된 이슈 ID에 해당하는 연결을 해제합니다.
    """
    issue_commands.unlink_issues(db, auth, cr.id, issue_ids=req.issue_ids)


# ── 담당자 동기화 ──


@router.put(
    "/{issue_number}/assignees",
    response_model=SyncAssigneesResponse,
    status_code=200,
)
def sync_assignees(
    req: SyncAssigneesRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 담당자 동기화.

    전달된 `user_ids`를 이슈의 최종 담당자 목록으로 설정합니다.
    기존 담당자와 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 담당자가 해제됩니다.
    """
    return issue_commands.sync_assignees(db, auth, cr.id, user_ids=req.user_ids)


# ── 검토자 동기화 ──


@router.put(
    "/{issue_number}/reviewers",
    response_model=SyncReviewersResponse,
    status_code=200,
)
def sync_reviewers(
    req: SyncReviewersRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """변경 요청 검토자 동기화.

    전달된 `user_ids`를 CR의 최종 검토자 목록으로 설정합니다.
    기존 검토자와 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 검토자가 해제됩니다.
    """
    return issue_commands.sync_reviewers(db, auth, cr.id, user_ids=req.user_ids)


# ── 라벨 동기화 ──


@router.put(
    "/{issue_number}/labels",
    response_model=SyncLabelsResponse,
    status_code=200,
)
def sync_labels(
    req: SyncLabelsRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 라벨 동기화.

    전달된 `label_ids`를 이슈의 최종 라벨 목록으로 설정합니다.
    기존 라벨과 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 라벨이 해제됩니다.
    """
    return issue_commands.sync_labels(db, auth, cr.id, label_ids=req.label_ids)


# ── 부품 동기화 ──


@router.put(
    "/{issue_number}/parts",
    response_model=SyncPartsResponse,
    status_code=200,
)
def sync_parts(
    req: SyncPartsRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 부품 동기화.

    전달된 `part_ids`를 이슈의 최종 부품 목록으로 설정합니다.
    기존 부품과 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 부품이 해제됩니다.
    """
    return issue_commands.sync_parts(
        db, auth, cr.project_id, cr.id, part_ids=req.part_ids
    )


# ── 타임라인 ──


@router.get(
    "/{issue_number}/timeline",
    response_model=TimelineResponse,
    status_code=200,
)
def get_timeline(
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 타임라인 조회.

    댓글과 활동 이력을 `created_at` 기준으로 시간순 merge하여 반환합니다.
    """
    return issue_queries.get_timeline(db, auth, cr.id)


# ── 댓글 ──


@router.post(
    "/{issue_number}/comments",
    response_model=CommentResponse,
    status_code=201,
)
def create_comment(
    req: CreateCommentRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 생성.

    댓글 본문은 1~10,000자까지 입력 가능합니다.
    """
    body = req.body.model_dump_json(exclude_none=True)
    return issue_commands.create_comment(db, auth, cr.id, body=body)


@router.patch(
    "/{issue_number}/comments/{comment_id}",
    response_model=CommentResponse,
    status_code=200,
)
def update_comment(
    comment_id: uuid.UUID,
    req: UpdateCommentRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 수정.

    본인이 작성한 댓글만 수정할 수 있습니다.
    """
    body = req.body.model_dump_json(exclude_none=True)
    return issue_commands.update_comment(db, auth, cr.id, comment_id, body=body)


@router.delete(
    "/{issue_number}/comments/{comment_id}",
    status_code=204,
)
def delete_comment(
    comment_id: uuid.UUID,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 삭제.

    본인이 작성한 댓글만 삭제할 수 있습니다.
    """
    issue_commands.delete_comment(db, auth, cr.id, comment_id)


# ── 첨부파일 ──


@router.post(
    "/{issue_number}/files",
    response_model=list[FileItem],
    status_code=200,
)
def add_files(
    req: AttachFilesRequest,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈에 첨부파일 배치 연결.

    업로드 완료(`UPLOADED`) 상태이며 아직 소유자가 없는 파일만 연결 가능합니다.
    최대 20개까지 한 번에 연결할 수 있습니다.
    """
    return issue_commands.add_files(db, auth, cr.id, file_ids=req.file_ids)


@router.delete(
    "/{issue_number}/files/{file_id}",
    status_code=204,
)
def delete_file(
    file_id: uuid.UUID,
    cr: ChangeRequest = Depends(resolve_change_request),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 첨부파일 1건 삭제.

    해당 이슈에 연결된 파일만 삭제할 수 있습니다.
    파일은 소프트 삭제 처리됩니다.
    """
    issue_commands.delete_file(db, auth, cr.id, file_id)
