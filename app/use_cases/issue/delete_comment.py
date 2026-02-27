"""이슈 댓글 삭제 — 소유권 및 작성자 검증 후 삭제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.issue import service as issue_service


@transactional()
def delete_comment(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    comment_id: uuid.UUID,
) -> None:
    """이슈 댓글 삭제."""
    issue_service.get_or_raise(db, issue_id)
    comment = issue_service.get_comment_or_raise(db, comment_id)
    if comment.issue_id != issue_id:
        raise AppError(message="해당 이슈의 댓글이 아닙니다", code="NOT_FOUND")
    if comment.created_by != auth.user_id:
        raise AppError(message="본인이 작성한 댓글만 삭제할 수 있습니다", code="FORBIDDEN")
    issue_service.delete_comment(db, comment)
