"""이슈(Issue) 도메인 Repository."""

import uuid

from sqlalchemy import String, cast, func, or_
from sqlalchemy import text as sa_text
from sqlalchemy.orm import Session

from app.modules.file.models import File
from app.modules.issue.constants import CRState, IssueState, IssueType, ReviewStatus
from app.modules.issue.models import (
    ChangeRequest,
    ChangeRequestIssue,
    ChangeRequestReviewer,
    ChangeRequestTeamReviewer,
    Issue,
    IssueAssignee,
    IssueComment,
    IssueLabel,
    IssuePart,
    IssueTeamAssignee,
)
from app.modules.label.models import Label
from app.modules.part.models import Part
from app.modules.team.models import Team, TeamMember


def get_next_number(db: Session) -> int:
    """테넌트 전역 다음 이슈 번호 채번 (동시성 안전).

    pg_advisory_xact_lock으로 직렬화하고 MAX(number)+1로 채번한다.
    트랜잭션 종료 시 잠금이 자동 해제된다.
    """
    db.execute(sa_text("SELECT pg_advisory_xact_lock(1)"))
    max_number = db.query(func.max(Issue.number)).scalar()
    return (max_number or 0) + 1


def count_issues_by_state(db: Session) -> dict[str, int]:
    """Issue 상태별 건수 조회 (CR 제외)."""
    rows = (
        db.query(Issue.state, func.count())
        .filter(Issue.type == IssueType.ISSUE)
        .group_by(Issue.state)
        .all()
    )
    counts = {s.value: 0 for s in IssueState}
    for state, cnt in rows:
        counts[state.value] = cnt
    return counts


def count_crs_by_state(db: Session) -> dict[str, int]:
    """ChangeRequest 이슈 상태별 건수 조회."""
    rows = (
        db.query(Issue.state, func.count())
        .filter(Issue.type == IssueType.CHANGE_REQUEST)
        .group_by(Issue.state)
        .all()
    )
    counts = {s.value: 0 for s in IssueState}
    for state, cnt in rows:
        counts[state.value] = cnt
    return counts


def list_issues_paginated(
    db: Session,
    *,
    state: str | None = None,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[Issue], int]:
    """Issue 목록 페이징 조회 (CR 제외)."""
    query = db.query(Issue).filter(
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
    *,
    state: str | None = None,
    cr_state: str | None = None,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[ChangeRequest], int]:
    """ChangeRequest 목록 페이징 조회."""
    query = db.query(ChangeRequest)
    if state:
        query = query.filter(ChangeRequest.state == state)
    if cr_state:
        query = query.filter(ChangeRequest.cr_state == cr_state)
    if search:
        query = query.filter(ChangeRequest.title.ilike(f"%{search}%"))
    total = query.count()
    rows = (
        query.order_by(ChangeRequest.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
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


def batch_load_reviewer_ids(
    db: Session, cr_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[uuid.UUID]]:
    """CR ID 목록에 대한 검토자 ID 배치 조회."""
    if not cr_ids:
        return {}
    rows = (
        db.query(ChangeRequestReviewer.change_request_id, ChangeRequestReviewer.user_id)
        .filter(ChangeRequestReviewer.change_request_id.in_(cr_ids))
        .all()
    )
    result: dict[uuid.UUID, list[uuid.UUID]] = {}
    for cr_id, user_id in rows:
        result.setdefault(cr_id, []).append(user_id)
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


def lookup_issues(
    db: Session,
    *,
    search: str | None = None,
    type: IssueType | None = None,
    limit: int = 10,
) -> list[Issue]:
    """이슈 lookup 조회 (picker/autocomplete용)."""
    query = db.query(Issue)
    if type is not None:
        query = query.filter(Issue.type == type)
    if search:
        conditions = [Issue.title.ilike(f"%{search}%")]
        if search.isdigit():
            conditions.append(cast(Issue.number, String).like(f"%{search}%"))
        query = query.filter(or_(*conditions))
    return query.order_by(Issue.number.desc()).limit(limit).all()


def lookup_change_requests(
    db: Session,
    *,
    search: str | None = None,
    limit: int = 10,
) -> list[ChangeRequest]:
    """변경 요청 lookup 조회 (picker/autocomplete용)."""
    query = db.query(ChangeRequest)
    if search:
        conditions = [ChangeRequest.title.ilike(f"%{search}%")]
        if search.isdigit():
            conditions.append(cast(ChangeRequest.number, String).like(f"%{search}%"))
        query = query.filter(or_(*conditions))
    return query.order_by(ChangeRequest.number.desc()).limit(limit).all()


def get_by_id(db: Session, issue_id: uuid.UUID) -> Issue | None:
    """Issue 단건 조회."""
    return db.query(Issue).filter(Issue.id == issue_id).first()


def get_by_number(db: Session, number: int) -> Issue | None:
    """이슈 번호로 단건 조회."""
    return db.query(Issue).filter(Issue.number == number).first()


def get_cr_by_id(db: Session, issue_id: uuid.UUID) -> ChangeRequest | None:
    """ChangeRequest 단건 조회."""
    return db.query(ChangeRequest).filter(ChangeRequest.id == issue_id).first()


def get_cr_by_number(db: Session, number: int) -> ChangeRequest | None:
    """이슈 번호로 ChangeRequest 단건 조회."""
    return db.query(ChangeRequest).filter(ChangeRequest.number == number).first()


def add(db: Session, entity: Issue) -> Issue:
    """Issue(또는 ChangeRequest) 저장."""
    db.add(entity)
    db.flush()
    return entity


def sync_assignees(
    db: Session, issue_id: uuid.UUID, user_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 담당자 동기화 — diff 기반으로 추가/제거 수행, (added, removed) 반환.

    팀 배정으로 이미 커버된 user_id는 추가 대상에서 스킵합니다.
    """
    current = set(
        row[0]
        for row in db.query(IssueAssignee.user_id)
        .filter(IssueAssignee.issue_id == issue_id)
        .all()
    )
    desired = set(user_ids)

    # 팀 배정으로 커버된 user_id 조합
    covered_by_team = set(
        row[0]
        for row in db.query(TeamMember.user_id)
        .join(IssueTeamAssignee, TeamMember.team_id == IssueTeamAssignee.team_id)
        .filter(IssueTeamAssignee.issue_id == issue_id)
        .all()
    )

    to_add = desired - current - covered_by_team
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


def sync_reviewers(
    db: Session, cr_id: uuid.UUID, user_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """CR 검토자 동기화 — diff 기반으로 추가/제거 수행, (added, removed) 반환.

    팀 검토자로 이미 커버된 user_id는 추가 대상에서 스킵합니다.
    """
    current = set(
        row[0]
        for row in db.query(ChangeRequestReviewer.user_id)
        .filter(ChangeRequestReviewer.change_request_id == cr_id)
        .all()
    )
    desired = set(user_ids)

    # 팀 검토자로 커버된 user_id 조합
    covered_by_team = set(
        row[0]
        for row in db.query(TeamMember.user_id)
        .join(
            ChangeRequestTeamReviewer,
            TeamMember.team_id == ChangeRequestTeamReviewer.team_id,
        )
        .filter(ChangeRequestTeamReviewer.change_request_id == cr_id)
        .all()
    )

    to_add = desired - current - covered_by_team
    to_remove = current - desired

    if to_remove:
        db.query(ChangeRequestReviewer).filter(
            ChangeRequestReviewer.change_request_id == cr_id,
            ChangeRequestReviewer.user_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for uid in to_add:
        db.add(ChangeRequestReviewer(change_request_id=cr_id, user_id=uid))

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


def sync_issues(
    db: Session, cr_id: uuid.UUID, issue_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """CR-Issue 연결 동기화 — diff 기반으로 추가/제거 수행, (added, removed) 반환."""
    current = set(
        row[0]
        for row in db.query(ChangeRequestIssue.issue_id)
        .filter(ChangeRequestIssue.change_request_id == cr_id)
        .all()
    )
    desired = set(issue_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(ChangeRequestIssue).filter(
            ChangeRequestIssue.change_request_id == cr_id,
            ChangeRequestIssue.issue_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for iid in to_add:
        db.add(ChangeRequestIssue(change_request_id=cr_id, issue_id=iid))

    if to_add or to_remove:
        db.flush()

    return list(to_add), list(to_remove)


def sync_changes(
    db: Session, issue_id: uuid.UUID, cr_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """Issue-CR 연결 동기화 (역방향) — diff 기반으로 추가/제거 수행, (added, removed) 반환."""
    current = set(
        row[0]
        for row in db.query(ChangeRequestIssue.change_request_id)
        .filter(ChangeRequestIssue.issue_id == issue_id)
        .all()
    )
    desired = set(cr_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(ChangeRequestIssue).filter(
            ChangeRequestIssue.issue_id == issue_id,
            ChangeRequestIssue.change_request_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for cid in to_add:
        db.add(ChangeRequestIssue(change_request_id=cid, issue_id=issue_id))

    if to_add or to_remove:
        db.flush()

    return list(to_add), list(to_remove)


def list_linked_issue_ids(db: Session, cr_id: uuid.UUID) -> list[uuid.UUID]:
    """CR에 연결된 이슈 ID 목록 조회."""
    rows = (
        db.query(ChangeRequestIssue.issue_id)
        .filter(ChangeRequestIssue.change_request_id == cr_id)
        .all()
    )
    return [row[0] for row in rows]


def batch_load_linked_issues(
    db: Session, cr_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[Issue]]:
    """CR ID 목록에 대해 연결된 Issue를 배치 조회."""
    if not cr_ids:
        return {}
    rows = (
        db.query(ChangeRequestIssue.change_request_id, Issue)
        .join(Issue, Issue.id == ChangeRequestIssue.issue_id)
        .filter(ChangeRequestIssue.change_request_id.in_(cr_ids))
        .all()
    )
    result: dict[uuid.UUID, list[Issue]] = {cid: [] for cid in cr_ids}
    for cr_id, issue in rows:
        result[cr_id].append(issue)
    return result


def batch_load_linked_crs(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[ChangeRequest]]:
    """Issue ID 목록에 대해 연결된 ChangeRequest를 배치 조회."""
    if not issue_ids:
        return {}
    rows = (
        db.query(ChangeRequestIssue.issue_id, ChangeRequest)
        .join(ChangeRequest, ChangeRequest.id == ChangeRequestIssue.change_request_id)
        .filter(ChangeRequestIssue.issue_id.in_(issue_ids))
        .all()
    )
    result: dict[uuid.UUID, list[ChangeRequest]] = {iid: [] for iid in issue_ids}
    for issue_id, cr in rows:
        result[issue_id].append(cr)
    return result


def has_unresolved_linked_crs(db: Session, issue_id: uuid.UUID) -> bool:
    """이슈에 MERGED/CLOSED가 아닌 연결된 CR이 존재하는지 확인."""
    count = (
        db.query(func.count(ChangeRequestIssue.id))
        .join(ChangeRequest, ChangeRequest.id == ChangeRequestIssue.change_request_id)
        .filter(
            ChangeRequestIssue.issue_id == issue_id,
            ChangeRequest.cr_state.notin_([CRState.MERGED, CRState.CLOSED]),
        )
        .scalar()
    )
    return count > 0


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


# ── 팀 담당자 (Issue) ──


def sync_team_assignees(
    db: Session, issue_id: uuid.UUID, team_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """이슈 팀 담당자 동기화 — diff 기반 추가/제거, (added, removed) 반환.

    추가된 팀 멤버와 겹치는 개인 IssueAssignee는 자동 제거합니다.
    """
    current = set(
        row[0]
        for row in db.query(IssueTeamAssignee.team_id)
        .filter(IssueTeamAssignee.issue_id == issue_id)
        .all()
    )
    desired = set(team_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(IssueTeamAssignee).filter(
            IssueTeamAssignee.issue_id == issue_id,
            IssueTeamAssignee.team_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for tid in to_add:
        db.add(IssueTeamAssignee(issue_id=issue_id, team_id=tid))

    if to_add or to_remove:
        db.flush()

    # 추가된 팀 멤버와 겹치는 개인 배정 자동 제거
    if to_add:
        overlapping_user_ids = set(
            row[0]
            for row in db.query(TeamMember.user_id)
            .filter(TeamMember.team_id.in_(to_add))
            .all()
        )
        if overlapping_user_ids:
            db.query(IssueAssignee).filter(
                IssueAssignee.issue_id == issue_id,
                IssueAssignee.user_id.in_(overlapping_user_ids),
            ).delete(synchronize_session="fetch")
            db.flush()

    return list(to_add), list(to_remove)


# ── 팀 검토자 (CR) ──


def sync_team_reviewers(
    db: Session, cr_id: uuid.UUID, team_ids: list[uuid.UUID]
) -> tuple[list[uuid.UUID], list[uuid.UUID]]:
    """CR 팀 검토자 동기화 — diff 기반 추가/제거, (added, removed) 반환.

    추가된 팀 멤버와 겹치는 개인 ChangeRequestReviewer는 자동 제거합니다.
    """
    current = set(
        row[0]
        for row in db.query(ChangeRequestTeamReviewer.team_id)
        .filter(ChangeRequestTeamReviewer.change_request_id == cr_id)
        .all()
    )
    desired = set(team_ids)
    to_add = desired - current
    to_remove = current - desired

    if to_remove:
        db.query(ChangeRequestTeamReviewer).filter(
            ChangeRequestTeamReviewer.change_request_id == cr_id,
            ChangeRequestTeamReviewer.team_id.in_(to_remove),
        ).delete(synchronize_session="fetch")

    for tid in to_add:
        db.add(ChangeRequestTeamReviewer(change_request_id=cr_id, team_id=tid))

    if to_add or to_remove:
        db.flush()

    # 추가된 팀 멤버와 겹치는 개인 검토자 자동 제거
    if to_add:
        overlapping_user_ids = set(
            row[0]
            for row in db.query(TeamMember.user_id)
            .filter(TeamMember.team_id.in_(to_add))
            .all()
        )
        if overlapping_user_ids:
            db.query(ChangeRequestReviewer).filter(
                ChangeRequestReviewer.change_request_id == cr_id,
                ChangeRequestReviewer.user_id.in_(overlapping_user_ids),
            ).delete(synchronize_session="fetch")
            db.flush()

    return list(to_add), list(to_remove)


# ── 리뷰 상태 ──


def update_review_status(
    db: Session,
    cr_id: uuid.UUID,
    user_id: uuid.UUID,
    status: ReviewStatus,
    now,
) -> ChangeRequestReviewer:
    """검토자의 리뷰 상태를 업데이트한다."""
    reviewer = (
        db.query(ChangeRequestReviewer)
        .filter(
            ChangeRequestReviewer.change_request_id == cr_id,
            ChangeRequestReviewer.user_id == user_id,
        )
        .first()
    )
    if not reviewer:
        from app.core.exceptions import AppError

        raise AppError(
            message="해당 변경 요청의 검토자가 아닙니다",
            code="FORBIDDEN",
        )
    reviewer.review_status = status.value
    reviewer.reviewed_at = now
    db.flush()
    return reviewer


# ── 팀 배치 로드 (enrichment용) ──


def batch_load_team_assignee_ids(
    db: Session, issue_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[uuid.UUID]]:
    """이슈 ID 목록에 대한 팀 담당자 ID 배치 조회."""
    if not issue_ids:
        return {}
    rows = (
        db.query(IssueTeamAssignee.issue_id, IssueTeamAssignee.team_id)
        .filter(IssueTeamAssignee.issue_id.in_(issue_ids))
        .all()
    )
    result: dict[uuid.UUID, list[uuid.UUID]] = {}
    for issue_id, team_id in rows:
        result.setdefault(issue_id, []).append(team_id)
    return result


def batch_load_team_reviewer_ids(
    db: Session, cr_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[uuid.UUID]]:
    """CR ID 목록에 대한 팀 검토자 ID 배치 조회."""
    if not cr_ids:
        return {}
    rows = (
        db.query(
            ChangeRequestTeamReviewer.change_request_id,
            ChangeRequestTeamReviewer.team_id,
        )
        .filter(ChangeRequestTeamReviewer.change_request_id.in_(cr_ids))
        .all()
    )
    result: dict[uuid.UUID, list[uuid.UUID]] = {}
    for cr_id, team_id in rows:
        result.setdefault(cr_id, []).append(team_id)
    return result


def batch_load_reviewer_details(
    db: Session, cr_ids: list[uuid.UUID]
) -> dict[uuid.UUID, list[ChangeRequestReviewer]]:
    """CR ID 목록에 대한 검토자 상세 배치 조회 (review_status 포함)."""
    if not cr_ids:
        return {}
    rows = (
        db.query(ChangeRequestReviewer)
        .filter(ChangeRequestReviewer.change_request_id.in_(cr_ids))
        .all()
    )
    result: dict[uuid.UUID, list[ChangeRequestReviewer]] = {}
    for reviewer in rows:
        result.setdefault(reviewer.change_request_id, []).append(reviewer)
    return result


def batch_load_teams(db: Session, team_ids: list[uuid.UUID]) -> dict[uuid.UUID, Team]:
    """Team ID 목록에 대한 Team 배치 조회."""
    if not team_ids:
        return {}
    teams = db.query(Team).filter(Team.id.in_(team_ids)).all()
    return {t.id: t for t in teams}
