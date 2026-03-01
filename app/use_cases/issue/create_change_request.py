"""변경 요청 생성 — 프로젝트 존재 검증 후 ChangeRequest 생성."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse
from app.modules.project import service as project_service


@transactional()
def create_change_request(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    title: str,
    body: str | None = None,
    issue_number: int | None = None,
) -> ChangeRequestResponse:
    """변경 요청 생성.

    issue_number가 주어지면 해당 이슈(ISSUE 타입)를 CR에 연결합니다.
    """
    project_service.get_or_raise(db, project_id)
    cr = issue_service.create_change_request(db, project_id, title, body)
    if issue_number is not None:
        issue = issue_service.get_issue_by_number_or_raise(db, project_id, issue_number)
        issue_service.link_issues(db, cr, [issue.id])
    return mapper.to_change_request_response(cr)
