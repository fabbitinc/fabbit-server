"""부품 lookup 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import repository as part_repo
from app.modules.part.schemas import PartLookupItem, PartLookupResponse


@transactional(read_only=True)
def lookup_parts(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    limit: int = 10,
) -> PartLookupResponse:
    """부품 lookup 조회 (picker/autocomplete용)."""
    parts = part_repo.lookup_parts(db, search=search, limit=limit)
    items = [
        PartLookupItem(id=p.id, part_number=p.part_number, name=p.name)
        for p in parts
    ]
    return PartLookupResponse(items=items)
