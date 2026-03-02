"""변경 요청 생성."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse
from app.modules.label import service as label_service


@transactional()
def create_change_request(
    db: Session,
    auth: AuthContext,  # noqa: ARG001
    title: str,
    body: str | None = None,
    issue_number: int | None = None,
    *,
    part_ids: list[uuid.UUID] | None = None,
    assignee_user_ids: list[uuid.UUID] | None = None,
    team_assignee_ids: list[uuid.UUID] | None = None,
    label_ids: list[uuid.UUID] | None = None,
    file_ids: list[uuid.UUID] | None = None,
    reviewer_user_ids: list[uuid.UUID] | None = None,
    team_reviewer_ids: list[uuid.UUID] | None = None,
) -> ChangeRequestResponse:
    """변경 요청 생성.

    issue_number가 주어지면 해당 이슈(ISSUE 타입)를 CR에 연결합니다.
    연관 데이터(부품, 담당자, 라벨, 파일, 검토자)가 전달되면
    동일 트랜잭션 내에서 일괄 연결합니다.
    """
    cr = issue_service.create_change_request(db, title, body)

    if issue_number is not None:
        issue = issue_service.get_issue_by_number_or_raise(db, issue_number)
        issue_service.link_issues(db, cr, [issue.id], emit_event=False)

    # 공통 (Issue와 동일)
    if part_ids:
        issue_service.sync_parts(db, cr, part_ids, emit_event=False)
    if assignee_user_ids:
        issue_service.sync_assignees(db, cr, assignee_user_ids, emit_event=False)
    if team_assignee_ids:
        issue_service.sync_team_assignees(db, cr, team_assignee_ids, emit_event=False)
    if label_ids:
        for lid in label_ids:
            label_service.get_or_raise(db, lid)
        issue_service.sync_labels(db, cr, label_ids, emit_event=False)
    if file_ids:
        files = file_service.validate_attachable(db, file_ids)
        issue_service.attach_files(db, cr.id, files, emit_event=False)

    # CR 특화
    if reviewer_user_ids:
        issue_service.sync_reviewers(db, cr, reviewer_user_ids, emit_event=False)
    if team_reviewer_ids:
        issue_service.sync_team_reviewers(db, cr, team_reviewer_ids, emit_event=False)

    return mapper.to_change_request_response(cr)
