"""라벨 lookup 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import repository as label_repo
from app.modules.label.schemas import LabelLookupItem, LabelLookupResponse


@transactional(read_only=True)
def lookup_labels(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    limit: int = 10,
) -> LabelLookupResponse:
    """라벨 lookup 조회 (picker/autocomplete용)."""
    labels = label_repo.lookup_labels(db, search=search, limit=limit)
    items = [
        LabelLookupItem(id=lb.id, name=lb.name, color=lb.color)
        for lb in labels
    ]
    return LabelLookupResponse(items=items)
