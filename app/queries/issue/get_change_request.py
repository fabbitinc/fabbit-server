"""변경 요청 상세 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.issue import mapper
from app.modules.issue import repository as repo
from app.modules.issue.schemas import ChangeRequestResponse
from app.queries.issue._enrichment import load_enrichments


@transactional(read_only=True)
def get_change_request(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    issue_number: int,
) -> ChangeRequestResponse:
    """ChangeRequest 상세 조회 (프로젝트 + 번호 기반)."""
    cr = repo.get_cr_by_project_and_number(db, project_id, issue_number)
    if not cr:
        raise AppError(
            message=f"ChangeRequest #{issue_number}을(를) 찾을 수 없습니다",
            code="NOT_FOUND",
        )

    enrichments = load_enrichments(db, [cr])
    e = enrichments[cr.id]

    return mapper.to_change_request_response(
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
