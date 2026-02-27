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
) -> ChangeRequestResponse:
    """변경 요청 생성."""
    project_service.get_or_raise(db, project_id)
    cr = issue_service.create_change_request(db, project_id, title, body)
    return mapper.to_change_request_response(cr)
