"""slug 사용 가능 여부 확인."""

import uuid

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.organization import repository as org_repo
from app.modules.organization.constants import validate_slug_format
from app.modules.organization.schemas import CheckSlugResponse


@transactional(read_only=True)
def check_slug(db: Session, slug: str) -> CheckSlugResponse:
    """slug 사용 가능 여부 확인."""
    # 1. 포맷 검증
    error = validate_slug_format(slug)
    if error:
        return CheckSlugResponse(available=False, message=error)
    # 2. DB 중복 확인
    if org_repo.get_org_by_slug(db, slug):
        suggestion = f"{slug}-{str(uuid.uuid4())[:4]}"
        return CheckSlugResponse(
            available=False,
            message="이미 사용 중인 워크스페이스 주소입니다",
            suggestion=suggestion,
        )
    return CheckSlugResponse(available=True)
