"""변경 요청 생성."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def create_change_request(
    db: Session,
    auth: AuthContext,
    title: str,
    body: str | None = None,
    issue_number: int | None = None,
) -> ChangeRequestResponse:
    """변경 요청 생성.

    issue_number가 주어지면 해당 이슈(ISSUE 타입)를 CR에 연결합니다.
    """
    cr = issue_service.create_change_request(db, title, body)
    if issue_number is not None:
        issue = issue_service.get_issue_by_number_or_raise(db, issue_number)
        issue_service.link_issues(db, cr, [issue.id])
    return mapper.to_change_request_response(cr)
