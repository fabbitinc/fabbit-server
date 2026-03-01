"""변경 요청 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper
from app.modules.issue import repository as repo
from app.modules.issue.schemas import ChangeRequestListResponse
from app.queries.issue._enrichment import load_enrichments


@transactional(read_only=True)
def list_change_requests(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    *,
    state: str | None = None,
    cr_state: str | None = None,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> ChangeRequestListResponse:
    """프로젝트 내 ChangeRequest 목록 페이징 조회."""
    state_counts = repo.count_crs_by_state(db, project_id)
    rows, total = repo.list_crs_paginated(
        db,
        project_id,
        state=state,
        cr_state=cr_state,
        search=search,
        offset=offset,
        limit=limit,
    )

    enrichments = load_enrichments(db, rows)
    items = []
    for cr in rows:
        e = enrichments[cr.id]
        items.append(
            mapper.to_cr_summary(
                cr,
                created_by_name=e.created_by_name,
                created_by_profile_image_url=e.created_by_profile_image_url,
                labels=e.labels,
                assignees=e.assignees,
                reviewers=e.reviewers,
                parts=e.parts,
                files=e.files,
                comments_count=e.comments_count,
            )
        )

    return ChangeRequestListResponse(
        open_count=state_counts["OPEN"],
        closed_count=state_counts["CLOSED"],
        total=total,
        offset=offset,
        limit=limit,
        items=items,
    )
