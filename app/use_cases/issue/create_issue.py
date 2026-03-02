"""이슈 생성."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import IssueResponse
from app.modules.label import service as label_service


@transactional()
def create_issue(
    db: Session,
    auth: AuthContext,  # noqa: ARG001
    title: str,
    body: str | None = None,
    *,
    part_ids: list[uuid.UUID] | None = None,
    assignee_user_ids: list[uuid.UUID] | None = None,
    team_assignee_ids: list[uuid.UUID] | None = None,
    label_ids: list[uuid.UUID] | None = None,
    file_ids: list[uuid.UUID] | None = None,
) -> IssueResponse:
    """이슈 생성.

    연관 데이터(부품, 담당자, 라벨, 파일)가 전달되면
    동일 트랜잭션 내에서 일괄 연결합니다.
    """
    issue = issue_service.create_issue(db, title, body)

    if part_ids:
        issue_service.sync_parts(db, issue, part_ids)
    if assignee_user_ids:
        issue_service.sync_assignees(db, issue, assignee_user_ids)
    if team_assignee_ids:
        issue_service.sync_team_assignees(db, issue, team_assignee_ids)
    if label_ids:
        for lid in label_ids:
            label_service.get_or_raise(db, lid)
        issue_service.sync_labels(db, issue, label_ids)
    if file_ids:
        files = file_service.validate_attachable(db, file_ids)
        issue_service.attach_files(db, issue.id, files)

    return mapper.to_issue_response(issue)
