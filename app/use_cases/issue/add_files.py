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
    files = file_service.validate_attachable(db, file_ids)
    issue_service.attach_files(db, issue_id, files)
    return to_file_items(files)
