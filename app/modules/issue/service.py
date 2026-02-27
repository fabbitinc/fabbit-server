"""이슈(Issue) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.modules.issue import repository as repo
from app.modules.issue.models import ChangeRequest, Issue


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
