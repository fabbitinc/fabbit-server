"""스토리지 사용량 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.file import repository as file_repo
from app.modules.organization.models import Organization
from app.modules.usage.schemas import (
    StorageCategory,
    StorageCategoryItem,
    StorageUsageResponse,
)


@transactional(read_only=True)
def get_storage_usage(db: Session, auth: AuthContext) -> StorageUsageResponse:
    """Organization 스토리지 총량 + 카테고리별 내역 조회."""
    org = db.query(
        Organization.storage_bytes_used,
        Organization.storage_bytes_limit,
        Organization.allow_storage_overage,
    ).filter(Organization.id == auth.org_id).one()

    breakdown = file_repo.get_storage_breakdown(db)

    categories = [
        StorageCategoryItem(
            category=StorageCategory(category),
            bytes_used=bytes_used,
            file_count=file_count,
        )
        for category, file_count, bytes_used in breakdown
    ]

    overage = max(org.storage_bytes_used - org.storage_bytes_limit, 0)

    return StorageUsageResponse(
        bytes_used=org.storage_bytes_used,
        bytes_limit=org.storage_bytes_limit,
        bytes_overage=overage,
        allow_overage=org.allow_storage_overage,
        categories=categories,
    )
