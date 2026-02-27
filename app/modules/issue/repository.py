"""이슈(Issue) 도메인 Repository."""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.modules.issue.models import Issue
from app.modules.project.models import Project


def get_next_number(db: Session, project_id: uuid.UUID) -> int:
    """프로젝트 내 다음 이슈 번호 조회 (동시성 안전).

    Project 행을 FOR UPDATE로 잠가 동일 프로젝트 내 채번을 직렬화한다.
    """
    db.query(Project).filter(Project.id == project_id).with_for_update().one()
    max_num = (
        db.query(func.max(Issue.number))
        .filter(Issue.project_id == project_id)
        .scalar()
    )
    return (max_num or 0) + 1


def add(db: Session, entity: Issue) -> Issue:
    """Issue(또는 ChangeRequest) 저장."""
    db.add(entity)
    db.flush()
    return entity
