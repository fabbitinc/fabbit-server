"""이슈 댓글 수정 — 소유권 및 작성자 검증 후 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import CommentResponse


@transactional()
def update_comment(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    comment_id: uuid.UUID,
    body: str,
) -> CommentResponse:
    """이슈 댓글 수정."""
    issue = issue_service.get_or_raise(db, issue_id)
    comment = issue_service.get_comment_or_raise(db, comment_id)
    if comment.issue_id != issue_id:
        raise AppError(message="해당 이슈의 댓글이 아닙니다", code="NOT_FOUND")
    if comment.created_by != auth.user_id:
        raise AppError(message="본인이 작성한 댓글만 수정할 수 있습니다", code="FORBIDDEN")
    comment = issue_service.update_comment(db, issue, comment, body)
    return mapper.to_comment_response(comment)
