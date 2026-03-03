"""카테고리 선택용 경량 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.schemas import CategoryLookupResponse


@transactional(read_only=True)
def lookup_categories(db: Session, auth: AuthContext) -> CategoryLookupResponse:
    """카테고리 선택용 경량 목록 조회."""
    categories = repo.get_distinct_categories(db)
    return CategoryLookupResponse(items=categories)
