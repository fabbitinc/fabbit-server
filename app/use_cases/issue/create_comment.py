"""이슈 댓글 생성 — 이슈 존재 검증 후 댓글 생성."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.issue import mapper, service as issue_service
from app.modules.issue.schemas import CommentResponse


@transactional()
def create_comment(
    db: Session,
    auth: AuthContext,
    issue_id: uuid.UUID,
    body: str,
) -> CommentResponse:
    """이슈 댓글 생성."""
    issue_service.get_or_raise(db, issue_id)
    comment = issue_service.create_comment(db, issue_id, body)
    return mapper.to_comment_response(comment)
