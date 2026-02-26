"""Part 첨부파일 1건 삭제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import service as part_service


@transactional()
def delete_file(
    db: Session,
    auth: AuthContext,
    part_id: uuid.UUID,
    file_id: uuid.UUID,
) -> None:
    """Part 첨부파일 1건 삭제."""
    part_service.detach_file(db, part_id, file_id)
