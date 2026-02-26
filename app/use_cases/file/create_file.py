"""단건 파일 업로드 URL 발급."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.schemas import CreateFileRequest, CreateFileResponse


@transactional()
def create_file(
    db: Session, auth: AuthContext, req: CreateFileRequest
) -> CreateFileResponse:
    return file_service.create_file(db, auth, req)
