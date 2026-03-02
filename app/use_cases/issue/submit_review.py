"""변경 요청 리뷰 제출."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import service as issue_service
from app.modules.issue.schemas import SubmitReviewResponse


@transactional()
def submit_review(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    status: str,
) -> SubmitReviewResponse:
    """CR 리뷰 제출 — 본인의 review_status 업데이트."""
    cr = issue_service.get_cr_or_raise(db, issue_id)
    reviewer = issue_service.submit_review(db, cr, auth.user_id, status)
    return SubmitReviewResponse(
        review_status=reviewer.review_status,
        reviewed_at=reviewer.reviewed_at,
    )
