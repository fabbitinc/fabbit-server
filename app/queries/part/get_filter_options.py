"""Part 필터 옵션 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.schemas import PartFilterOptions


@transactional(read_only=True)
def get_filter_options(db: Session, auth: AuthContext) -> PartFilterOptions:
    """Part 필터 옵션 조회 — 카테고리, 수명주기 상태의 DISTINCT 값."""
    return PartFilterOptions(
        categories=repo.get_distinct_categories(db),
        lifecycle_states=repo.get_distinct_lifecycle_states(db),
    )
