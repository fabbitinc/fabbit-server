"""이슈 도메인 모델 → Pydantic 응답 변환."""

import json

from app.modules.file.schemas import FileItem
from app.modules.issue.models import ChangeRequest, Issue, IssueComment
from app.modules.issue.schemas import (
    AssigneeSummary,
    ChangeRequestResponse,
    ChangeRequestSummary,
    CommentResponse,
    IssueResponse,
    IssueSummary,
    LabelBadge,
    PartBadge,
)


def _parse_body(body: str | None) -> dict | None:
    """TEXT 컬럼의 body JSON 문자열을 dict로 변환."""
    if not body:
        return None
    try:
        return json.loads(body)
    except (json.JSONDecodeError, TypeError):
        return None


def to_issue_response(
    issue: Issue,
    *,
    created_by_name: str | None = None,
    created_by_profile_image_url: str | None = None,
    labels: list[LabelBadge] | None = None,
    assignees: list[AssigneeSummary] | None = None,
    parts: list[PartBadge] | None = None,
    files: list[FileItem] | None = None,
    comments_count: int = 0,
) -> IssueResponse:
    """Issue 모델 → IssueResponse 변환."""
    return IssueResponse(
        id=issue.id,
        project_id=issue.project_id,
        number=issue.number,
        type=issue.type.value,
        title=issue.title,
        body=_parse_body(issue.body),
        state=issue.state.value,
        closed_at=issue.closed_at,
        created_at=issue.created_at,
        updated_at=issue.updated_at,
        created_by=issue.created_by,
        created_by_name=created_by_name,
        created_by_profile_image_url=created_by_profile_image_url,
        labels=labels or [],
        assignees=assignees or [],
        parts=parts or [],
        files=files or [],
        comments_count=comments_count,
    )


def to_change_request_response(
    cr: ChangeRequest,
    *,
    created_by_name: str | None = None,
    created_by_profile_image_url: str | None = None,
    labels: list[LabelBadge] | None = None,
    assignees: list[AssigneeSummary] | None = None,
    reviewers: list[AssigneeSummary] | None = None,
    parts: list[PartBadge] | None = None,
    files: list[FileItem] | None = None,
    comments_count: int = 0,
) -> ChangeRequestResponse:
    """ChangeRequest 모델 → ChangeRequestResponse 변환."""
    return ChangeRequestResponse(
        id=cr.id,
        project_id=cr.project_id,
        number=cr.number,
        type=cr.type.value,
        title=cr.title,
        body=_parse_body(cr.body),
        state=cr.state.value,
        closed_at=cr.closed_at,
        created_at=cr.created_at,
        updated_at=cr.updated_at,
        created_by=cr.created_by,
        created_by_name=created_by_name,
        created_by_profile_image_url=created_by_profile_image_url,
        labels=labels or [],
        assignees=assignees or [],
        reviewers=reviewers or [],
        parts=parts or [],
        files=files or [],
        comments_count=comments_count,
        cr_state=cr.cr_state.value,
        merged_at=cr.merged_at,
        merged_by=cr.merged_by,
    )


def to_issue_summary(
    issue: Issue,
    *,
    created_by_name: str | None = None,
    created_by_profile_image_url: str | None = None,
    labels: list[LabelBadge] | None = None,
    assignees: list[AssigneeSummary] | None = None,
    parts: list[PartBadge] | None = None,
    files: list[FileItem] | None = None,
    comments_count: int = 0,
) -> IssueSummary:
    """Issue 모델 → IssueSummary 변환 (body 제외)."""
    return IssueSummary(
        id=issue.id,
        project_id=issue.project_id,
        number=issue.number,
        type=issue.type.value,
        title=issue.title,
        state=issue.state.value,
        closed_at=issue.closed_at,
        created_at=issue.created_at,
        updated_at=issue.updated_at,
        created_by=issue.created_by,
        created_by_name=created_by_name,
        created_by_profile_image_url=created_by_profile_image_url,
        labels=labels or [],
        assignees=assignees or [],
        parts=parts or [],
        files=files or [],
        comments_count=comments_count,
    )


def to_cr_summary(
    cr: ChangeRequest,
    *,
    created_by_name: str | None = None,
    created_by_profile_image_url: str | None = None,
    labels: list[LabelBadge] | None = None,
    assignees: list[AssigneeSummary] | None = None,
    reviewers: list[AssigneeSummary] | None = None,
    parts: list[PartBadge] | None = None,
    files: list[FileItem] | None = None,
    comments_count: int = 0,
) -> ChangeRequestSummary:
    """ChangeRequest 모델 → ChangeRequestSummary 변환 (body 제외)."""
    return ChangeRequestSummary(
        id=cr.id,
        project_id=cr.project_id,
        number=cr.number,
        type=cr.type.value,
        title=cr.title,
        state=cr.state.value,
        closed_at=cr.closed_at,
        created_at=cr.created_at,
        updated_at=cr.updated_at,
        created_by=cr.created_by,
        created_by_name=created_by_name,
        created_by_profile_image_url=created_by_profile_image_url,
        labels=labels or [],
        assignees=assignees or [],
        reviewers=reviewers or [],
        parts=parts or [],
        files=files or [],
        comments_count=comments_count,
        cr_state=cr.cr_state.value,
        merged_at=cr.merged_at,
        merged_by=cr.merged_by,
    )


def to_comment_response(comment: IssueComment) -> CommentResponse:
    """IssueComment 모델 → CommentResponse 변환."""
    return CommentResponse(
        id=comment.id,
        issue_id=comment.issue_id,
        body=_parse_body(comment.body),
        created_at=comment.created_at,
        updated_at=comment.updated_at,
        created_by=comment.created_by,
    )
