"""이슈 첨부파일 배치 연결."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.mapper import to_file_items
from app.modules.file.schemas import FileItem
from app.modules.issue import service as issue_service


@transactional()
def add_files(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    file_ids: list[uuid.UUID],
) -> list[FileItem]:
    """이슈에 첨부파일 배치 연결."""
    issue_service.get_or_raise(db, issue_id)
    files = file_service.attach_to_owner(db, file_ids, "issue", issue_id)
    return to_file_items(files)
