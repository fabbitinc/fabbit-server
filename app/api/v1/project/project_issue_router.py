"""이슈(Issue) API 라우터."""

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth, resolve_issue
from app.core.auth_context import AuthContext
from app.modules.activity.schemas import TimelineResponse
from app.modules.file.schemas import FileItem
from app.modules.issue.models import Issue
from app.modules.issue.schemas import (
    AttachFilesRequest,
    CommentResponse,
    CreateCommentRequest,
    CreateIssueRequest,
    IssueListResponse,
    IssueLookupResponse,
    IssueResponse,
    SyncAssigneesRequest,
    SyncAssigneesResponse,
    SyncLabelsRequest,
    SyncLabelsResponse,
    SyncPartsRequest,
    SyncPartsResponse,
    UpdateCommentRequest,
)
from app.queries import issue as issue_queries
from app.use_cases import issue as issue_commands

router = APIRouter(prefix="/api/v1/projects/{project_id}/issues", tags=["issues"])


@router.get("/lookup", response_model=IssueLookupResponse)
def lookup_issues(
    project_id: uuid.UUID,
    search: str | None = Query(None, description="제목 검색 (ILIKE)"),
    limit: int = Query(10, ge=1, le=50, description="조회 건수"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """프로젝트 이슈 lookup 조회.

    이슈 연결 picker UI를 위한 경량 목록 엔드포인트입니다.
    id, number, title, state만 반환합니다 (변경 요청 제외).
    """
    return issue_queries.lookup_issues(db, auth, project_id, search=search, limit=limit)


@router.get("", response_model=IssueListResponse)
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


@router.get("/{issue_number}", response_model=IssueResponse)
def get_issue(
    project_id: uuid.UUID,
    issue_number: int,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 상세 조회.

    프로젝트 내 이슈 번호(`number`)로 조회합니다.
    라벨, 담당자, 댓글 수, 작성자 이름 등 상세 정보를 포함합니다.
    """
    return issue_queries.get_issue(db, auth, project_id, issue_number)


@router.post("", response_model=IssueResponse, status_code=201)
def create_issue(
    project_id: uuid.UUID,
    req: CreateIssueRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 생성.

    프로젝트 내 고유 번호(`number`)가 자동 부여됩니다.
    """
    body = req.body.model_dump_json(exclude_none=True) if req.body else None
    return issue_commands.create_issue(db, auth, project_id, title=req.title, body=body)


# ── 담당자 동기화 ──


@router.put(
    "/{issue_number}/assignees",
    response_model=SyncAssigneesResponse,
    status_code=200,
)
def sync_assignees(
    req: SyncAssigneesRequest,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 담당자 동기화.

    전달된 `user_ids`를 이슈의 최종 담당자 목록으로 설정합니다.
    기존 담당자와 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 담당자가 해제됩니다.
    """
    return issue_commands.sync_assignees(db, auth, issue.id, user_ids=req.user_ids)


# ── 이슈 상태 전이 ──


@router.post(
    "/{issue_number}/close",
    response_model=IssueResponse,
    status_code=200,
)
def close_issue(
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 닫기.

    이슈 상태를 **OPEN → CLOSED**로 전환합니다.
    `closed_at` 타임스탬프가 자동 기록됩니다.
    """
    return issue_commands.close_issue(db, auth, issue.id)


@router.post(
    "/{issue_number}/reopen",
    response_model=IssueResponse,
    status_code=200,
)
def reopen_issue(
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 재개.

    닫힌 이슈 상태를 **CLOSED → OPEN**으로 전환합니다.
    `closed_at`이 초기화됩니다.
    """
    return issue_commands.reopen_issue(db, auth, issue.id)


# ── 라벨 동기화 ──


@router.put(
    "/{issue_number}/labels",
    response_model=SyncLabelsResponse,
    status_code=200,
)
def sync_labels(
    req: SyncLabelsRequest,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 라벨 동기화.

    전달된 `label_ids`를 이슈의 최종 라벨 목록으로 설정합니다.
    기존 라벨과 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 라벨이 해제됩니다.
    """
    return issue_commands.sync_labels(db, auth, issue.id, label_ids=req.label_ids)


# ── 부품 동기화 ──


@router.put(
    "/{issue_number}/parts",
    response_model=SyncPartsResponse,
    status_code=200,
)
def sync_parts(
    req: SyncPartsRequest,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 부품 동기화.

    전달된 `part_ids`를 이슈의 최종 부품 목록으로 설정합니다.
    기존 부품과 diff를 비교하여 추가/제거를 자동 처리합니다.
    빈 목록 전달 시 모든 부품이 해제됩니다.
    """
    return issue_commands.sync_parts(
        db, auth, issue.project_id, issue.id, part_ids=req.part_ids
    )


# ── 타임라인 ──


@router.get(
    "/{issue_number}/timeline",
    response_model=TimelineResponse,
    status_code=200,
)
def get_timeline(
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 타임라인 조회.

    댓글과 활동 이력을 `created_at` 기준으로 시간순 merge하여 반환합니다.
    """
    return issue_queries.get_timeline(db, auth, issue.id)


# ── 댓글 ──


@router.post(
    "/{issue_number}/comments",
    response_model=CommentResponse,
    status_code=201,
)
def create_comment(
    req: CreateCommentRequest,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 생성.

    댓글 본문은 1~10,000자까지 입력 가능합니다.
    """
    body = req.body.model_dump_json(exclude_none=True)
    return issue_commands.create_comment(db, auth, issue.id, body=body)


@router.patch(
    "/{issue_number}/comments/{comment_id}",
    response_model=CommentResponse,
    status_code=200,
)
def update_comment(
    comment_id: uuid.UUID,
    req: UpdateCommentRequest,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 수정.

    본인이 작성한 댓글만 수정할 수 있습니다.
    """
    body = req.body.model_dump_json(exclude_none=True)
    return issue_commands.update_comment(db, auth, issue.id, comment_id, body=body)


@router.delete(
    "/{issue_number}/comments/{comment_id}",
    status_code=204,
)
def delete_comment(
    comment_id: uuid.UUID,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 댓글 삭제.

    본인이 작성한 댓글만 삭제할 수 있습니다.
    """
    issue_commands.delete_comment(db, auth, issue.id, comment_id)


# ── 첨부파일 ──


@router.post(
    "/{issue_number}/files",
    response_model=list[FileItem],
    status_code=200,
)
def add_files(
    req: AttachFilesRequest,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈에 첨부파일 배치 연결.

    업로드 완료(`UPLOADED`) 상태이며 아직 소유자가 없는 파일만 연결 가능합니다.
    최대 20개까지 한 번에 연결할 수 있습니다.
    """
    return issue_commands.add_files(db, auth, issue.id, file_ids=req.file_ids)


@router.delete(
    "/{issue_number}/files/{file_id}",
    status_code=204,
)
def delete_file(
    file_id: uuid.UUID,
    issue: Issue = Depends(resolve_issue),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """이슈 첨부파일 1건 삭제.

    해당 이슈에 연결된 파일만 삭제할 수 있습니다.
    파일은 소프트 삭제 처리됩니다.
    """
    issue_commands.delete_file(db, auth, issue.id, file_id)
