"""단건 업로드 완료 확인."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.schemas import FileCompleteResponse


@transactional()
def complete_file(
    db: Session, file_id: uuid.UUID, auth: AuthContext
) -> FileCompleteResponse:
    return file_service.complete_file(db, file_id, auth)
