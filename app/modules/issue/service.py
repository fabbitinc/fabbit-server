"""이슈(Issue) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.file.models import File
from app.modules.issue import repository as repo
from app.modules.issue.events import (
    AssigneesChanged,
    CRCreated,
    CRIssuesLinked,
    CRIssuesUnlinked,
    IssueCreated,
    IssueFileDetached,
    IssueFilesAttached,
    IssueLabelsChanged,
    IssueMentioned,
    IssuePartsChanged,
    ReviewersChanged,
    UserMentioned,
)
from app.modules.issue.constants import IssueType
from app.modules.issue.mention import extract_mentions
from app.modules.issue.models import ChangeRequest, Issue, IssueComment


def _register_mention_events(
    issue: Issue,
    new_body: str | None,
    old_body: str | None = None,
    *,
    is_comment: bool = False,
) -> None:
    """new_body에서 새로 추가된 mention을 추출하여 이벤트 등록."""
    new_users, new_issues = extract_mentions(new_body)
    old_users, old_issues = extract_mentions(old_body)
    added_users = new_users - old_users
    added_issues = new_issues - old_issues
    # 자기 자신 멘션 제외
    added_issues.discard(issue.id)

    issue_type = issue.type.value.lower()  # "issue" | "change_request"
    for target_issue_id in added_issues:
        issue.register_event(
            IssueMentioned(
                project_id=issue.project_id,
                target_issue_id=target_issue_id,
                source_issue_id=issue.id,
                source_number=issue.number,
                source_title=issue.title,
                source_issue_type=issue_type,
                is_comment=is_comment,
            )
        )
    for mentioned_user_id in added_users:
        issue.register_event(
            UserMentioned(
                project_id=issue.project_id,
                mentioned_user_id=mentioned_user_id,
                source_issue_id=issue.id,
                source_number=issue.number,
                source_title=issue.title,
                source_issue_type=issue_type,
                is_comment=is_comment,
            )
        )


def get_or_raise(db: Session, issue_id: uuid.UUID) -> Issue:
    """Issue 조회 — 없으면 AppError(NOT_FOUND)."""
    issue = repo.get_by_id(db, issue_id)
    if not issue:
        raise AppError(
            message=f"Issue '{issue_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )
    return issue


def get_issue_by_number_or_raise(
    db: Session, project_id: uuid.UUID, number: int
) -> Issue:
    """프로젝트 내 이슈 번호로 ISSUE 타입 조회 — 없거나 타입 불일치 시 AppError."""
    issue = repo.get_by_project_and_number(db, project_id, number)
    if not issue or issue.type != IssueType.ISSUE:
        raise AppError(
            message=f"이슈 #{number}을(를) 찾을 수 없습니다", code="NOT_FOUND"
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
    issue.register_event(
        IssueCreated(
            project_id=project_id,
            issue_id=issue.id,
            number=issue.number,
            title=title,
        )
    )
    _register_mention_events(issue, body)
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
    cr.register_event(
        CRCreated(
            project_id=project_id,
            issue_id=cr.id,
            number=cr.number,
            title=title,
        )
    )
    _register_mention_events(cr, body)
    return cr


def update_issue(
    db: Session,
    issue: Issue,
    title: str | None = None,
    body: str | None = None,
) -> Issue:
    """이슈 제목/본문 수정."""
    old_body = issue.body if body is not None else None
    if title is not None:
        issue.title = title
    if body is not None:
        issue.body = body
        _register_mention_events(issue, body, old_body)
    db.flush()
    return issue


def attach_files(db: Session, issue_id: uuid.UUID, files: list[File]) -> None:
    """Issue에 검증된 파일들을 연결."""
    issue = get_or_raise(db, issue_id)
    issue.attach_files(files)
    if files:
        issue.register_event(
            IssueFilesAttached(
                issue_id=issue.id,
                files=[
                    {"file_id": str(f.id), "original_name": f.original_name}
                    for f in files
                ],
            )
        )


def detach_file(db: Session, issue_id: uuid.UUID, file_id: uuid.UUID) -> None:
    """Issue 첨부파일 1건 분리."""
    issue = get_or_raise(db, issue_id)
    # 스냅샷용 파일명 조회 (분리 전)
    file = db.query(File).filter(File.id == file_id).first()
    file_name = file.original_name if file else "(알 수 없음)"
    issue.detach_file(file_id)
    issue.register_event(
        IssueFileDetached(
            issue_id=issue.id,
            file_id=file_id,
            file_name=file_name,
        )
    )


def sync_assignees(
    db: Session, issue: Issue, user_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 담당자 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_assignees(db, issue.id, user_ids)
    if added or removed:
        from app.modules.user.models import User

        all_ids = list(set(added) | set(removed))
        users = {u.id: u for u in db.query(User).filter(User.id.in_(all_ids)).all()}

        def _snap(uid: uuid.UUID) -> dict:
            u = users.get(uid)
            return {"user_id": str(uid), "name": u.full_name if u else "(알 수 없음)"}

        issue.register_event(
            AssigneesChanged(
                issue_id=issue.id,
                added=[_snap(uid) for uid in added],
                removed=[_snap(uid) for uid in removed],
            )
        )
    return added, removed


def sync_reviewers(
    db: Session, cr: ChangeRequest, user_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """CR 검토자 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_reviewers(db, cr.id, user_ids)
    if added or removed:
        from app.modules.user.models import User

        all_ids = list(set(added) | set(removed))
        users = {u.id: u for u in db.query(User).filter(User.id.in_(all_ids)).all()}

        def _snap(uid: uuid.UUID) -> dict:
            u = users.get(uid)
            return {"user_id": str(uid), "name": u.full_name if u else "(알 수 없음)"}

        cr.register_event(
            ReviewersChanged(
                issue_id=cr.id,
                added=[_snap(uid) for uid in added],
                removed=[_snap(uid) for uid in removed],
            )
        )
    return added, removed


def sync_labels(
    db: Session, issue: Issue, label_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 라벨 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_labels(db, issue.id, label_ids)
    if added or removed:
        from app.modules.label.models import Label

        all_ids = list(set(added) | set(removed))
        labels = {
            lb.id: lb for lb in db.query(Label).filter(Label.id.in_(all_ids)).all()
        }

        def _snap(lid: uuid.UUID) -> dict:
            lb = labels.get(lid)
            if lb:
                return {"label_id": str(lid), "name": lb.name, "color": lb.color}
            return {"label_id": str(lid), "name": "(삭제됨)", "color": "#888888"}

        issue.register_event(
            IssueLabelsChanged(
                issue_id=issue.id,
                added=[_snap(lid) for lid in added],
                removed=[_snap(lid) for lid in removed],
            )
        )
    return added, removed


def sync_parts(
    db: Session, issue: Issue, part_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 부품 동기화 — (added, removed) 반환."""
    added, removed = repo.sync_parts(db, issue.id, part_ids)
    if added or removed:
        from app.modules.part.models import Part

        all_ids = list(set(added) | set(removed))
        parts = {
            p.id: p for p in db.query(Part).filter(Part.id.in_(all_ids)).all()
        }

        def _snap(pid: uuid.UUID) -> dict:
            p = parts.get(pid)
            return {
                "part_id": str(pid),
                "part_number": p.part_number if p else "(알 수 없음)",
            }

        issue.register_event(
            IssuePartsChanged(
                issue_id=issue.id,
                added=[_snap(pid) for pid in added],
                removed=[_snap(pid) for pid in removed],
            )
        )
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


def link_issues(db: Session, cr: ChangeRequest, issue_ids: list[uuid.UUID]) -> int:
    """CR에 이슈 배치 연결 — 신규 연결 건수 반환."""
    count = repo.link_issues(db, cr.id, issue_ids)
    if count > 0:
        # 스냅샷용 이슈 조회
        issues = db.query(Issue).filter(Issue.id.in_(issue_ids)).all()
        cr.register_event(
            CRIssuesLinked(
                issue_id=cr.id,
                cr_number=cr.number,
                cr_title=cr.title,
                linked_issues=[
                    {"issue_id": str(i.id), "number": i.number, "title": i.title, "type": i.type.value}
                    for i in issues
                ],
            )
        )
    return count


def unlink_issues(db: Session, cr: ChangeRequest, issue_ids: list[uuid.UUID]) -> int:
    """CR에서 이슈 배치 해제 — 삭제 건수 반환."""
    count = repo.unlink_issues(db, cr.id, issue_ids)
    if count > 0:
        # 스냅샷용 이슈 조회
        issues = db.query(Issue).filter(Issue.id.in_(issue_ids)).all()
        cr.register_event(
            CRIssuesUnlinked(
                issue_id=cr.id,
                cr_number=cr.number,
                cr_title=cr.title,
                unlinked_issues=[
                    {"issue_id": str(i.id), "number": i.number, "title": i.title, "type": i.type.value}
                    for i in issues
                ],
            )
        )
    return count


# ── 상태 전이 ──


def close_linked_open_issues(db: Session, cr: ChangeRequest) -> None:
    """CR에 연결된 열린 이슈 중, 모든 연결 CR이 완료된 이슈만 닫는다."""
    from datetime import datetime, timezone

    from app.modules.issue.constants import IssueState

    linked_ids = repo.list_linked_issue_ids(db, cr.id)
    now = datetime.now(timezone.utc)
    for lid in linked_ids:
        linked = repo.get_by_id(db, lid)
        if linked and linked.state == IssueState.OPEN:
            if not repo.has_unresolved_linked_crs(db, lid):
                linked.close(now)
    db.flush()


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


def merge_cr(db: Session, cr: ChangeRequest, user_id: uuid.UUID) -> ChangeRequest:
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


def create_comment(db: Session, issue: Issue, body: str) -> IssueComment:
    """댓글 생성."""
    comment = IssueComment(issue_id=issue.id, body=body)
    repo.add_comment(db, comment)
    _register_mention_events(issue, body, is_comment=True)
    return comment


def update_comment(
    db: Session, issue: Issue, comment: IssueComment, body: str
) -> IssueComment:
    """댓글 본문 수정."""
    old_body = comment.body
    comment.body = body
    _register_mention_events(issue, body, old_body, is_comment=True)
    db.flush()
    return comment


def delete_comment(db: Session, comment: IssueComment) -> None:
    """댓글 삭제."""
    repo.delete_comment(db, comment)
