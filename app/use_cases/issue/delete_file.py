"""이슈 첨부파일 1건 삭제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.issue import service as issue_service


@transactional()
def delete_file(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    file_id: uuid.UUID,
) -> None:
    """이슈 첨부파일 1건 삭제."""
    issue_service.get_or_raise(db, issue_id)
    file_service.detach_from_owner(db, "issue", issue_id, file_id)
