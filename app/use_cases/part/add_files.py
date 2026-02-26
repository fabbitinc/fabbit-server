"""Part에 첨부파일 배치 추가."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.mapper import to_file_items
from app.modules.file.schemas import FileItem
from app.modules.part import service as part_service


@transactional()
def add_files(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    file_ids: list[uuid.UUID],
) -> list[FileItem]:
    """완료된 파일들을 Part에 배치 추가."""
    files = file_service.validate_attachable(db, file_ids)
    part_service.attach_files(db, part_id, files)
    return to_file_items(files)
