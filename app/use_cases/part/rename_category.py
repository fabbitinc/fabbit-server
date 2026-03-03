"""카테고리 이름 일괄 변경."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import service as part_service


@transactional()
def rename_category(db: Session, auth: AuthContext, old_name: str, new_name: str) -> int:
    """카테고리 이름 일괄 변경 — 변경된 Part 건수 반환."""
    return part_service.rename_category(db, old_name, new_name)
