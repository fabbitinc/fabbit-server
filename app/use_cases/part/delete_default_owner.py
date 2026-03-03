"""기본 담당자/팀 설정 삭제."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import service as part_service


@transactional()
def delete_default_owner(
    db: Session,
    auth: AuthContext,
    category: str | None,
) -> None:
    """기본 담당자/팀 설정 삭제."""
    part_service.delete_default_owner(db, category)
