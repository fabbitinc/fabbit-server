"""서브도메인 워크스페이스 정보 조회."""

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.file.mapper import get_file_url
from app.modules.organization import repository as org_repo
from app.modules.organization.schemas import SiteResponse


@transactional(read_only=True)
def get_site(db: Session, slug: str | None) -> SiteResponse:
    """서브도메인 slug로 워크스페이스 기본 정보 조회."""
    if not slug:
        raise AppError(
            message="워크스페이스를 통해 접근해주세요", code="VALIDATION_ERROR"
        )
    org = org_repo.get_org_by_slug(db, slug)
    if not org:
        raise AppError(message="존재하지 않는 워크스페이스입니다", code="NOT_FOUND")
    return SiteResponse(
        slug=org.slug,
        name=org.name,
        profile_image_url=get_file_url(org.profile_image_file_key),
    )
