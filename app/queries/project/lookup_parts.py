"""프로젝트 부품 lookup 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import repository as part_repo
from app.modules.part.schemas import PartLookupItem, PartLookupResponse


@transactional(read_only=True)
def lookup_parts(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    *,
    search: str | None = None,
    exclude_linked: bool = False,
    limit: int = 10,
) -> PartLookupResponse:
    """프로젝트 부품 lookup 조회 (picker/autocomplete용)."""
    parts = part_repo.lookup_parts(
        db,
        search=search,
        exclude_project_id=project_id if exclude_linked else None,
        limit=limit,
    )
    items = [
        PartLookupItem(id=p.id, part_number=p.part_number, name=p.name)
        for p in parts
    ]
    return PartLookupResponse(items=items)
