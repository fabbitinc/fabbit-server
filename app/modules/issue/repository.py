"""이슈(Issue) 도메인 Repository."""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.modules.user.models import User
from app.modules.file.models import File
from app.modules.issue.constants import IssueState, IssueType
from app.modules.issue.models import (
    ChangeRequest,
    ChangeRequestIssue,
    Issue,
    IssueAssignee,
    IssueComment,
    IssueLabel,
    IssuePart,
)
from app.modules.label.models import Label
from app.modules.part.models import Part
from app.modules.project.models import Project


def get_next_number(db: Session, project_id: uuid.UUID) -> int:
    """프로젝트 내 다음 이슈 번호 채번 (동시성 안전).

    Project 행을 FOR UPDATE로 잠그고 카운터를 증가시켜 채번을 직렬화한다.
    """
    project = db.query(Project).filter(Project.id == project_id).with_for_update().one()
    return project.next_issue_number()


def count_issues_by_state(
    db: Session, project_id: uuid.UUID
) -> dict[str, int]:
    """프로젝트 내 Issue 상태별 건수 조회 (CR 제외)."""
    rows = (
        db.query(Issue.state, func.count())
        .filter(Issue.project_id == project_id, Issue.type == IssueType.ISSUE)
        .group_by(Issue.state)
        .all()
    )
    counts = {s.value: 0 for s in IssueState}
    for state, cnt in rows:
        counts[state.value] = cnt
    return counts


def count_crs_by_state(
    db: Session, project_id: uuid.UUID
) -> dict[str, int]:
    """프로젝트 내 ChangeRequest 이슈 상태별 건수 조회."""
    rows = (
        db.query(Issue.state, func.count())
        .filter(Issue.project_id == project_id, Issue.type == IssueType.CHANGE_REQUEST)
        .group_by(Issue.state)
        .all()
    )
    counts = {s.value: 0 for s in IssueState}
    for state, cnt in rows:
        counts[state.value] = cnt
    return counts


def list_issues_paginated(
    db: Session,
    project_id: uuid.UUID,
    *,
    state: str | None = None,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[Issue], int]:
    """프로젝트 내 Issue 목록 페이징 조회 (CR 제외)."""
    query = db.query(Issue).filter(
        Issue.project_id == project_id,
        Issue.type == IssueType.ISSUE,
    )
    if state:
        query = query.filter(Issue.state == state)
    if search:
        query = query.filter(Issue.title.ilike(f"%{search}%"))
    total = query.count()
    rows = query.order_by(Issue.created_at.desc()).offset(offset).limit(limit).all()
    return rows, total


def list_crs_paginated(
    db: Session,
    project_id: uuid.UUID,
    *,
    state: str | None = None,
    cr_state: str | None = None,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[ChangeRequest], int]:
    """프로젝트 내 ChangeRequest 목록 페이징 조회."""
    query = db.query(ChangeRequest).filter(
        ChangeRequest.project_id == project_id,
    )
    if state:
        query = query.filter(ChangeRequest.state == state)
    if cr_state:
        query = query.filter(ChangeRequest.cr_state == cr_state)
    if search:
        query = query.filter(ChangeRequest.title.ilike(f"%{search}%"))
    total = query.count()
    rows = query.order_by(ChangeRequest.created_at.desc()).offset(offset).limit(limit).all()
    return rows, total


# ── 목록 enrichment용 배치 조회 ──


def batch_load_labels(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[Label]]:
    """이슈 ID 목록에 대한 라벨 배치 조회."""
    rows = (
        db.query(IssueLabel.issue_id, Label)
        .join(Label, IssueLabel.label_id == Label.id)
        .filter(IssueLabel.issue_id.in_(issue_ids))
        .all()
    )
    result: dict[uuid.UUID, list[Label]] = {}
    for issue_id, label in rows:
        result.setdefault(issue_id, []).append(label)
    return result


def batch_load_assignee_ids(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[uuid.UUID]]:
    """이슈 ID 목록에 대한 담당자 ID 배치 조회."""
    rows = (
        db.query(IssueAssignee.issue_id, IssueAssignee.user_id)
        .filter(IssueAssignee.issue_id.in_(issue_ids))
        .all()
    )
    result: dict[uuid.UUID, list[uuid.UUID]] = {}
    for issue_id, user_id in rows:
        result.setdefault(issue_id, []).append(user_id)
    return result


def batch_load_comment_counts(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, int]:
    """이슈 ID 목록에 대한 댓글 수 배치 조회."""
    rows = (
        db.query(IssueComment.issue_id, func.count(IssueComment.id))
        .filter(IssueComment.issue_id.in_(issue_ids))
        .group_by(IssueComment.issue_id)
        .all()
    )
    return {issue_id: cnt for issue_id, cnt in rows}


def batch_load_user_names(
    db: Session, user_ids: list[uuid.UUID]
) -> dict[uuid.UUID, str]:
    """User ID 목록에 대한 이름 배치 조회 (cross-schema)."""
    if not user_ids:
        return {}
    rows = (
        db.query(User.id, User.full_name)
        .filter(User.id.in_(user_ids))
        .all()
    )
    return {uid: name for uid, name in rows}


def batch_load_parts(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[Part]]:
    """이슈 ID 목록에 대한 연결 부품 배치 조회."""
    rows = (
        db.query(IssuePart.issue_id, Part)
        .join(Part, IssuePart.part_id == Part.id)
        .filter(IssuePart.issue_id.in_(issue_ids))
        .all()
    )
    result: dict[uuid.UUID, list[Part]] = {}
    for issue_id, part in rows:
        result.setdefault(issue_id, []).append(part)
    return result


def batch_load_files(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[File]]:
    """이슈 ID 목록에 대한 첨부파일 배치 조회."""
    rows = (
        db.query(File)
        .filter(File.owner_type == "issue", File.owner_id.in_(issue_ids))
        .all()
    )
    result: dict[uuid.UUID, list[File]] = {}
    for f in rows:
        result.setdefault(f.owner_id, []).append(f)
    return result


def get_by_id(db: Session, issue_id: uuid.UUID) -> Issue | None:
    """Issue 단건 조회."""
    return db.query(Issue).filter(Issue.id == issue_id).first()


def get_cr_by_id(db: Session, issue_id: uuid.UUID) -> ChangeRequest | None:
    """ChangeRequest 단건 조회."""
    return db.query(ChangeRequest).filter(ChangeRequest.id == issue_id).first()


def add(db: Session, entity: Issue) -> Issue:
    """Issue(또는 ChangeRequest) 저장."""
    db.add(entity)
    db.flush()
    return entity


def sync_assignees(
    db: Session, issue_id: uuid.UUID, user_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 담당자 동기화 — diff 기반으로 추가/제거 수행, (added, removed) 반환."""
    current = set(
        row[0]
        for row in db.query(IssueAssignee.user_id)
        .filter(IssueAssignee.issue_id == issue_id)
        .all()
    )
    desired = set(user_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(IssueAssignee).filter(
            IssueAssignee.issue_id == issue_id,
            IssueAssignee.user_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for uid in to_add:
        db.add(IssueAssignee(issue_id=issue_id, user_id=uid))

    if to_add or to_remove:
        db.flush()

    return list(to_add), list(to_remove)


def sync_parts(
    db: Session, issue_id: uuid.UUID, part_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 부품 동기화 — diff 기반으로 추가/제거 수행, (added, removed) 반환."""
    current = set(
        row[0]
        for row in db.query(IssuePart.part_id)
        .filter(IssuePart.issue_id == issue_id)
        .all()
    )
    desired = set(part_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(IssuePart).filter(
            IssuePart.issue_id == issue_id,
            IssuePart.part_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for pid in to_add:
        db.add(IssuePart(issue_id=issue_id, part_id=pid))

    if to_add or to_remove:
        db.flush()

    return list(to_add), list(to_remove)


# ── 라벨 연결 ──


def sync_labels(
    db: Session, issue_id: uuid.UUID, label_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 라벨 동기화 — diff 기반으로 추가/제거 수행, (added, removed) 반환."""
    current = set(
        row[0]
        for row in db.query(IssueLabel.label_id)
        .filter(IssueLabel.issue_id == issue_id)
        .all()
    )
    desired = set(label_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(IssueLabel).filter(
            IssueLabel.issue_id == issue_id,
            IssueLabel.label_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for lid in to_add:
        db.add(IssueLabel(issue_id=issue_id, label_id=lid))

    if to_add or to_remove:
        db.flush()

    return list(to_add), list(to_remove)


# ── CR-Issue 연결 ──


def link_issues(
    db: Session, cr_id: uuid.UUID, issue_ids: list[uuid.UUID]
) -> int:
    """CR에 이슈 배치 연결 — 이미 연결된 건은 무시, 신규 연결 건수 반환."""
    existing = set(
        row[0]
        for row in db.query(ChangeRequestIssue.issue_id)
        .filter(
            ChangeRequestIssue.change_request_id == cr_id,
            ChangeRequestIssue.issue_id.in_(issue_ids),
        )
        .all()
    )
    new_ids = [iid for iid in issue_ids if iid not in existing]
    for iid in new_ids:
        db.add(ChangeRequestIssue(change_request_id=cr_id, issue_id=iid))
    if new_ids:
        db.flush()
    return len(new_ids)


def unlink_issues(
    db: Session, cr_id: uuid.UUID, issue_ids: list[uuid.UUID]
) -> int:
    """CR에서 이슈 배치 해제 — 삭제 건수 반환."""
    count = (
        db.query(ChangeRequestIssue)
        .filter(
            ChangeRequestIssue.change_request_id == cr_id,
            ChangeRequestIssue.issue_id.in_(issue_ids),
        )
        .delete(synchronize_session="fetch")
    )
    db.flush()
    return count


def list_linked_issue_ids(db: Session, cr_id: uuid.UUID) -> list[uuid.UUID]:
    """CR에 연결된 이슈 ID 목록 조회."""
    rows = (
        db.query(ChangeRequestIssue.issue_id)
        .filter(ChangeRequestIssue.change_request_id == cr_id)
        .all()
    )
    return [row[0] for row in rows]


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
