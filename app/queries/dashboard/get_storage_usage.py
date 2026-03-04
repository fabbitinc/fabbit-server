"""스토리지 사용량 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.dashboard.schemas import (
    StorageCategoryItem,
    StorageUsageResponse,
)
from app.modules.file import repository as file_repo
from app.modules.organization.models import Organization

DISPLAY_NAMES: dict[str, str] = {
    "drawing": "도면",
    "attachment": "첨부파일",
    "other": "기타",
}


@transactional(read_only=True)
def get_storage_usage(db: Session, auth: AuthContext) -> StorageUsageResponse:
    """Organization 스토리지 총량 + 카테고리별 내역 조회."""
    org = db.query(
        Organization.storage_bytes_used,
        Organization.storage_bytes_limit,
    ).filter(Organization.id == auth.org_id).one()

    breakdown = file_repo.get_storage_breakdown(db)

    categories = [
        StorageCategoryItem(
            category=category,
            display_name=DISPLAY_NAMES.get(category, category),
            bytes_used=bytes_used,
            file_count=file_count,
        )
        for category, file_count, bytes_used in breakdown
    ]

    return StorageUsageResponse(
        bytes_used=org.storage_bytes_used,
        bytes_limit=org.storage_bytes_limit,
        categories=categories,
    )
