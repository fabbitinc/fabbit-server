"""이슈(Issue) 도메인 Repository."""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.modules.issue.models import Issue, IssueAssignee, IssueComment, IssuePart
from app.modules.project.models import Project


def get_next_number(db: Session, project_id: uuid.UUID) -> int:
    """프로젝트 내 다음 이슈 번호 조회 (동시성 안전).

    Project 행을 FOR UPDATE로 잠가 동일 프로젝트 내 채번을 직렬화한다.
    """
    db.query(Project).filter(Project.id == project_id).with_for_update().one()
    max_num = (
        db.query(func.max(Issue.number)).filter(Issue.project_id == project_id).scalar()
    )
    return (max_num or 0) + 1


def get_by_id(db: Session, issue_id: uuid.UUID) -> Issue | None:
    """Issue 단건 조회."""
    return db.query(Issue).filter(Issue.id == issue_id).first()


def add(db: Session, entity: Issue) -> Issue:
    """Issue(또는 ChangeRequest) 저장."""
    db.add(entity)
    db.flush()
    return entity


def add_assignees(db: Session, issue_id: uuid.UUID, user_ids: list[uuid.UUID]) -> int:
    """이슈 담당자 배치 할당 — 이미 할당된 건은 무시, 신규 할당 건수 반환."""
    existing = set(
        row[0]
        for row in db.query(IssueAssignee.user_id)
        .filter(
            IssueAssignee.issue_id == issue_id,
            IssueAssignee.user_id.in_(user_ids),
        )
        .all()
    )
    new_ids = [uid for uid in user_ids if uid not in existing]
    for uid in new_ids:
        db.add(IssueAssignee(issue_id=issue_id, user_id=uid))
    if new_ids:
        db.flush()
    return len(new_ids)


def remove_assignees(
    db: Session, issue_id: uuid.UUID, user_ids: list[uuid.UUID]
) -> int:
    """이슈 담당자 배치 해제 — 삭제 건수 반환."""
    count = (
        db.query(IssueAssignee)
        .filter(
            IssueAssignee.issue_id == issue_id,
            IssueAssignee.user_id.in_(user_ids),
        )
        .delete(synchronize_session="fetch")
    )
    db.flush()
    return count


def link_parts(db: Session, issue_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """이슈에 부품 배치 연결 — 이미 연결된 건은 무시, 신규 연결 건수 반환."""
    existing = set(
        row[0]
        for row in db.query(IssuePart.part_id)
        .filter(
            IssuePart.issue_id == issue_id,
            IssuePart.part_id.in_(part_ids),
        )
        .all()
    )
    new_ids = [pid for pid in part_ids if pid not in existing]
    for pid in new_ids:
        db.add(IssuePart(issue_id=issue_id, part_id=pid))
    if new_ids:
        db.flush()
    return len(new_ids)


def unlink_parts(db: Session, issue_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """이슈에서 부품 배치 해제 — 삭제 건수 반환."""
    count = (
        db.query(IssuePart)
        .filter(
            IssuePart.issue_id == issue_id,
            IssuePart.part_id.in_(part_ids),
        )
        .delete(synchronize_session="fetch")
    )
    db.flush()
    return count


# ── 댓글 ──


def list_comments_by_issue(db: Session, issue_id: uuid.UUID) -> list[IssueComment]:
    """이슈별 댓글 목록 조회 (시간순)."""
    return (
        db.query(IssueComment)
        .filter(IssueComment.issue_id == issue_id)
        .order_by(IssueComment.created_at)
        .all()
    )


def get_comment_by_id(db: Session, comment_id: uuid.UUID) -> IssueComment | None:
    """댓글 단건 조회."""
    return db.query(IssueComment).filter(IssueComment.id == comment_id).first()


def add_comment(db: Session, entity: IssueComment) -> IssueComment:
    """댓글 저장."""
    db.add(entity)
    db.flush()
    return entity


def delete_comment(db: Session, comment: IssueComment) -> None:
    """댓글 하드 삭제."""
    db.delete(comment)
    db.flush()
