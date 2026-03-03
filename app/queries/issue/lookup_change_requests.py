"""변경 요청 lookup 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import repository as repo
from app.modules.issue.schemas import ChangeRequestLookupItem, ChangeRequestLookupResponse


@transactional(read_only=True)
def lookup_change_requests(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    limit: int = 10,
) -> ChangeRequestLookupResponse:
    """변경 요청 lookup 조회 (picker/autocomplete용)."""
    crs = repo.lookup_change_requests(db, search=search, limit=limit)
    items = [
        ChangeRequestLookupItem(
            id=cr.id,
            number=cr.number,
            title=cr.title,
            state=cr.state.value,
            cr_state=cr.cr_state.value,
        )
        for cr in crs
    ]
    return ChangeRequestLookupResponse(items=items)
