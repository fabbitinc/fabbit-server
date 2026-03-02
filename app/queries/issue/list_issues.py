"""이슈 목록 조회."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper
from app.modules.issue import repository as repo
from app.modules.issue.schemas import IssueListResponse
from app.queries.issue._enrichment import load_enrichments


@transactional(read_only=True)
def list_issues(
    db: Session,
    auth: AuthContext,
    *,
    state: str | None = None,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> IssueListResponse:
    """Issue 목록 페이징 조회 (CR 제외)."""
    state_counts = repo.count_issues_by_state(db)
    rows, total = repo.list_issues_paginated(
        db, state=state, search=search, offset=offset, limit=limit
    )

    enrichments = load_enrichments(db, rows)
    items = []
    for issue in rows:
        e = enrichments[issue.id]
        items.append(
            mapper.to_issue_summary(
                issue,
                created_by=e.created_by,
                labels=e.labels,
                assignees=e.assignees,
                parts=e.parts,
                files=e.files,
                comments_count=e.comments_count,
            )
        )

    return IssueListResponse(
        open_count=state_counts["OPEN"],
        closed_count=state_counts["CLOSED"],
        total=total,
        offset=offset,
        limit=limit,
        items=items,
    )
