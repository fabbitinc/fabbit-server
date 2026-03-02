"""라벨 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import mapper, repository as repo
from app.modules.label.schemas import LabelListResponse


@transactional(read_only=True)
def list_labels(
    db: Session,
    auth: AuthContext,
) -> LabelListResponse:
    """테넌트 전체 라벨 목록 조회."""
    labels = repo.list_all(db)
    items = [mapper.to_label_response(label) for label in labels]
    return LabelListResponse(total=len(items), items=items)
