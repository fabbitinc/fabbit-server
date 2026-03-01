"""프로필 이미지 설정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.mapper import get_file_url
from app.modules.user import service as user_service
from app.modules.user.schemas import ProfileImageResponse


@transactional()
def set_profile_image(
    db: Session,
    auth: AuthContext,
    file_id: uuid.UUID,
) -> ProfileImageResponse:
    """업로드 완료된 파일을 프로필 이미지로 설정."""
    files = file_service.validate_attachable(db, [file_id])
    user_service.set_profile_image(db, auth, files[0])
    return ProfileImageResponse(
        profile_image_url=get_file_url(files[0].file_key),
    )
