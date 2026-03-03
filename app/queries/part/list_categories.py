"""카테고리별 부품 개수 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.schemas import CategoryStatsItem, CategoryStatsResponse


@transactional(read_only=True)
def list_categories(db: Session, auth: AuthContext) -> CategoryStatsResponse:
    """카테고리 목록 + 부품 개수 조회."""
    rows = repo.get_category_stats(db)
    items = [CategoryStatsItem(category=cat, part_count=cnt) for cat, cnt in rows]
    return CategoryStatsResponse(items=items)
