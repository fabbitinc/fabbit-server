"""팀 lookup 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.team import repository as team_repo
from app.modules.team.schemas import TeamLookupItem, TeamLookupResponse


@transactional(read_only=True)
def lookup_teams(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    limit: int = 10,
) -> TeamLookupResponse:
    """팀 lookup 조회 (picker/autocomplete용)."""
    teams = team_repo.lookup_teams(db, search=search, limit=limit)
    items = [TeamLookupItem(id=t.id, name=t.name) for t in teams]
    return TeamLookupResponse(items=items)
