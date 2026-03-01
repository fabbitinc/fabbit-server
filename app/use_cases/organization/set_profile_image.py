"""조직 프로필 이미지 설정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import service as file_service
from app.modules.file.mapper import get_file_url
from app.modules.organization import service as org_service
from app.modules.organization.schemas import ProfileImageResponse


@transactional()
def set_profile_image(
    db: Session,
    auth: AuthContext,
    file_id: uuid.UUID,
) -> ProfileImageResponse:
    """업로드 완료된 파일을 조직 프로필 이미지로 설정.

    원본 이미지를 256x256 WebP 썸네일로 변환 후 저장.
    """
    files = file_service.validate_attachable(db, [file_id])
    file = files[0]
    file_service.convert_to_thumbnail(db, file)
    org_service.set_profile_image(db, auth, file)
    return ProfileImageResponse(
        profile_image_url=get_file_url(file.file_key),
    )
