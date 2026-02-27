"""이슈 도메인 모델 → Pydantic 응답 변환."""

from app.modules.issue.models import ChangeRequest, Issue, IssueComment
from app.modules.issue.schemas import ChangeRequestResponse, CommentResponse, IssueResponse


def to_issue_response(issue: Issue) -> IssueResponse:
    """Issue 모델 → IssueResponse 변환."""
    return IssueResponse(
        id=issue.id,
        project_id=issue.project_id,
        number=issue.number,
        type=issue.type.value,
        title=issue.title,
        body=issue.body,
        state=issue.state.value,
        closed_at=issue.closed_at,
        created_at=issue.created_at,
        created_by=issue.created_by,
    )


def to_change_request_response(cr: ChangeRequest) -> ChangeRequestResponse:
    """ChangeRequest 모델 → ChangeRequestResponse 변환."""
    return ChangeRequestResponse(
        id=cr.id,
        project_id=cr.project_id,
        number=cr.number,
        type=cr.type.value,
        title=cr.title,
        body=cr.body,
        state=cr.state.value,
        closed_at=cr.closed_at,
        created_at=cr.created_at,
        created_by=cr.created_by,
        cr_state=cr.cr_state.value,
        merged_at=cr.merged_at,
        merged_by=cr.merged_by,
    )


def to_comment_response(comment: IssueComment) -> CommentResponse:
    """IssueComment 모델 → CommentResponse 변환."""
    return CommentResponse(
        id=comment.id,
        issue_id=comment.issue_id,
        body=comment.body,
        created_at=comment.created_at,
        updated_at=comment.updated_at,
        created_by=comment.created_by,
    )
