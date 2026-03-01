"""조직 프로필 이미지 제거."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.organization import service as org_service


@transactional()
def delete_profile_image(
    db: Session,
    auth: AuthContext,
) -> None:
    """조직 프로필 이미지를 제거 — 소프트 삭제는 FileHandler가 처리."""
    files = file_service.get_files_by_owner(db, "organization", auth.org_id)
    if not files:
        return
    org_service.delete_profile_image(db, auth, files[0].id)
