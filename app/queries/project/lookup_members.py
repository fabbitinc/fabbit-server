"""프로젝트 멤버 lookup 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.project import repository as project_repo
from app.modules.project.schemas import MemberLookupResponse
from app.modules.user import mapper as user_mapper


@transactional(read_only=True)
def lookup_members(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    *,
    search: str | None = None,
    limit: int = 10,
) -> MemberLookupResponse:
    """프로젝트 멤버 lookup 조회 (picker/autocomplete용)."""
    users = project_repo.lookup_members(db, project_id, search=search, limit=limit)
    items = [user_mapper.to_user_summary(u) for u in users]
    return MemberLookupResponse(items=items)
