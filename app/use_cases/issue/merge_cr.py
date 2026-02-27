"""변경 요청 반영 — 연결된 열린 이슈 자동 닫기."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, repository as issue_repo, service as issue_service
from app.modules.issue.constants import IssueState
from app.modules.issue.schemas import ChangeRequestResponse


@transactional()
def merge_cr(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
) -> ChangeRequestResponse:
    """CR을 반영하고, 연결된 열린 이슈를 자동으로 닫는다."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    issue_service.merge_cr(db, cr, auth.user_id)
    # 연결된 열린 이슈 자동 닫기
    linked_ids = issue_repo.list_linked_issue_ids(db, cr.id)
    for lid in linked_ids:
        linked = issue_repo.get_by_id(db, lid)
        if linked and linked.state == IssueState.OPEN:
            issue_service.close_issue(db, linked)
    return mapper.to_change_request_response(cr)
