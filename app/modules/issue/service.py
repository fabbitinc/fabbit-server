"""이슈(Issue) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.issue import repository as repo
from app.modules.issue.models import ChangeRequest, Issue


def get_or_raise(db: Session, issue_id: uuid.UUID) -> Issue:
    """Issue 조회 — 없으면 AppError(NOT_FOUND)."""
    issue = repo.get_by_id(db, issue_id)
    if not issue:
        raise AppError(message=f"Issue '{issue_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")
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
    return repo.add(db, issue)


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
    return repo.add(db, cr)


def assign_users(db: Session, issue_id: uuid.UUID, user_ids: list[uuid.UUID]) -> int:
    """이슈 담당자 배치 할당 — 신규 할당 건수 반환."""
    return repo.add_assignees(db, issue_id, user_ids)


def unassign_users(db: Session, issue_id: uuid.UUID, user_ids: list[uuid.UUID]) -> int:
    """이슈 담당자 배치 해제 — 삭제 건수 반환."""
    return repo.remove_assignees(db, issue_id, user_ids)


def link_parts(db: Session, issue_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """이슈에 부품 배치 연결 — 신규 연결 건수 반환."""
    return repo.link_parts(db, issue_id, part_ids)


def unlink_parts(db: Session, issue_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """이슈에서 부품 배치 해제 — 삭제 건수 반환."""
    return repo.unlink_parts(db, issue_id, part_ids)
