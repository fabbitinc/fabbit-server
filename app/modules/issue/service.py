"""이슈(Issue) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.issue import repository as repo
from app.modules.issue.events import (
    AssigneesChanged,
    CRIssuesLinked,
    CRIssuesUnlinked,
    IssueCreated,
    IssueLabelsChanged,
    IssuePartsChanged,
)
from app.modules.issue.models import ChangeRequest, Issue, IssueComment


def get_or_raise(db: Session, issue_id: uuid.UUID) -> Issue:
    """Issue 조회 — 없으면 AppError(NOT_FOUND)."""
    issue = repo.get_by_id(db, issue_id)
    if not issue:
        raise AppError(
            message=f"Issue '{issue_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )
    return issue


def create_issue(
    db: Session,
    project_id: uuid.UUID,
    title: str,
    body: str | None = None,
) -> Issue:
    """일반 이슈 생성."""
    number = repo.get_next_number(db, project_id)
    issue = Issue(
        project_id=project_id,
        number=number,
        title=title,
        body=body,
    )
    repo.add(db, issue)
    issue.register_event(IssueCreated(
        project_id=project_id,
        issue_id=issue.id,
        number=issue.number,
        title=title,
        issue_type=issue.type.value,
    ))
    return issue


def create_change_request(
    db: Session,
    project_id: uuid.UUID,
    title: str,
    body: str | None = None,
) -> ChangeRequest:
    """변경 요청 생성."""
    number = repo.get_next_number(db, project_id)
    cr = ChangeRequest(
        project_id=project_id,
        number=number,
        title=title,
        body=body,
    )
    repo.add(db, cr)
    cr.register_event(IssueCreated(
        project_id=project_id,
        issue_id=cr.id,
        number=cr.number,
        title=title,
        issue_type=cr.type.value,
    ))
    return cr


def sync_assignees(
    db: Session, issue: Issue, user_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 담당자 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_assignees(db, issue.id, user_ids)
    if added or removed:
        issue.register_event(AssigneesChanged(
            issue_id=issue.id,
            added_user_ids=added,
            removed_user_ids=removed,
        ))
    return added, removed


def sync_labels(
    db: Session, issue: Issue, label_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 라벨 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_labels(db, issue.id, label_ids)
    if added or removed:
        issue.register_event(IssueLabelsChanged(
            issue_id=issue.id,
            added_label_ids=added,
            removed_label_ids=removed,
        ))
    return added, removed


def sync_parts(
    db: Session, issue: Issue, part_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 부품 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_parts(db, issue.id, part_ids)
    if added or removed:
        issue.register_event(IssuePartsChanged(
            issue_id=issue.id,
            added_part_ids=added,
            removed_part_ids=removed,
        ))
    return added, removed


# ── CR 조회 ──


def get_cr_or_raise(db: Session, issue_id: uuid.UUID) -> ChangeRequest:
    """ChangeRequest 조회 — 없거나 타입 불일치 시 AppError(NOT_FOUND)."""
    issue = repo.get_by_id(db, issue_id)
    if not issue or not isinstance(issue, ChangeRequest):
        raise AppError(
            message=f"ChangeRequest '{issue_id}'을(를) 찾을 수 없습니다",
            code="NOT_FOUND",
        )
    return issue


# ── CR-Issue 연결 ──


def link_issues(
    db: Session, cr: ChangeRequest, issue_ids: list[uuid.UUID]
) -> int:
    """CR에 이슈 배치 연결 — 신규 연결 건수 반환."""
    count = repo.link_issues(db, cr.id, issue_ids)
    if count > 0:
        cr.register_event(CRIssuesLinked(
            issue_id=cr.id, cr_number=cr.number, cr_title=cr.title,
            linked_issue_ids=issue_ids,
        ))
    return count


def unlink_issues(
    db: Session, cr: ChangeRequest, issue_ids: list[uuid.UUID]
) -> int:
    """CR에서 이슈 배치 해제 — 삭제 건수 반환."""
    count = repo.unlink_issues(db, cr.id, issue_ids)
    if count > 0:
        cr.register_event(CRIssuesUnlinked(
            issue_id=cr.id, cr_number=cr.number, cr_title=cr.title,
            unlinked_issue_ids=issue_ids,
        ))
    return count


# ── 상태 전이 ──


def close_issue(db: Session, issue: Issue) -> Issue:
    """이슈 닫기."""
    from datetime import datetime, timezone

    issue.close(datetime.now(timezone.utc))
    db.flush()
    return issue


def reopen_issue(db: Session, issue: Issue) -> Issue:
    """이슈 재개."""
    issue.reopen()
    db.flush()
    return issue


def open_cr_for_review(db: Session, cr: ChangeRequest) -> ChangeRequest:
    """CR 검토 상태로 전환."""
    cr.open_for_review()
    db.flush()
    return cr


def merge_cr(
    db: Session, cr: ChangeRequest, user_id: uuid.UUID
) -> ChangeRequest:
    """CR 반영."""
    from datetime import datetime, timezone

    cr.merge(datetime.now(timezone.utc), user_id)
    db.flush()
    return cr


def close_cr(db: Session, cr: ChangeRequest) -> ChangeRequest:
    """CR 닫기."""
    from datetime import datetime, timezone

    cr.close(datetime.now(timezone.utc))
    db.flush()
    return cr


# ── 댓글 ──


def get_comment_or_raise(db: Session, comment_id: uuid.UUID) -> IssueComment:
    """댓글 조회 — 없으면 AppError(NOT_FOUND)."""
    comment = repo.get_comment_by_id(db, comment_id)
    if not comment:
        raise AppError(
            message=f"댓글 '{comment_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )
    return comment


def create_comment(db: Session, issue_id: uuid.UUID, body: str) -> IssueComment:
    """댓글 생성."""
    comment = IssueComment(issue_id=issue_id, body=body)
    return repo.add_comment(db, comment)


def update_comment(db: Session, comment: IssueComment, body: str) -> IssueComment:
    """댓글 본문 수정."""
    comment.body = body
    db.flush()
    return comment


def delete_comment(db: Session, comment: IssueComment) -> None:
    """댓글 삭제."""
    repo.delete_comment(db, comment)
