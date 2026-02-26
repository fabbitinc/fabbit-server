"""배치 업로드 URL 발급."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.schemas import BatchCreateFileRequest, BatchCreateFileResponse


@transactional()
def batch_create_files(
    db: Session, auth: AuthContext, req: BatchCreateFileRequest
) -> BatchCreateFileResponse:
    return file_service.batch_create_files(db, auth, req)
