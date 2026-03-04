"""배치 업로드 완료 확인."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.schemas import BatchCompleteRequest, BatchCompleteResponse


@transactional()
def batch_complete_files(
    db: Session, auth: AuthContext, req: BatchCompleteRequest
) -> BatchCompleteResponse:
    return file_service.batch_complete_files(db, req)
