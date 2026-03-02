"""이슈 lookup 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import repository as repo
from app.modules.issue.constants import IssueType
from app.modules.issue.schemas import IssueLookupItem, IssueLookupResponse


@transactional(read_only=True)
def lookup_issues(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    type: IssueType | None = None,
    limit: int = 10,
) -> IssueLookupResponse:
    """이슈 lookup 조회 (picker/autocomplete용)."""
    issues = repo.lookup_issues(db, search=search, type=type, limit=limit)
    items = [
        IssueLookupItem(
            id=issue.id,
            number=issue.number,
            title=issue.title,
            state=issue.state.value,
            type=issue.type.value,
        )
        for issue in issues
    ]
    return IssueLookupResponse(items=items)
