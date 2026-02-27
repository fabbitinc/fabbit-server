"""Part 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.part import repository as repo
from app.modules.part.schemas import PartListResponse, PartSummary


@transactional(read_only=True)
def list_parts(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    category: str | None = None,
    lifecycle_state: str | None = None,
    has_drawing: bool | None = None,
    has_children: bool | None = None,
    project_id: uuid.UUID | None = None,
    offset: int = 0,
    limit: int = 20,
) -> PartListResponse:
    rows, total = repo.list_parts_paginated(
        db,
        search=search,
        category=category,
        lifecycle_state=lifecycle_state,
        has_drawing=has_drawing,
        has_children=has_children,
        project_id=project_id,
        offset=offset,
        limit=limit,
    )

    items = [PartSummary(**r) for r in rows]

    return PartListResponse(total=total, offset=offset, limit=limit, items=items)
