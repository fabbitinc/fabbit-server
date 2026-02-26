"""프로젝트(Project) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.project import repository as repo
from app.modules.project.models import Project


def get_or_raise(db: Session, project_id: uuid.UUID) -> Project:
    """Project 조회 — 없으면 AppError(NOT_FOUND)."""
    project = repo.get_project_by_id(db, project_id)
    if not project:
        raise AppError(message=f"Project '{project_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")
    return project


def link_parts(db: Session, project_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """Project에 Part 배치 연결 — 신규 연결 건수 반환."""
    return repo.link_parts(db, project_id, part_ids)


def unlink_parts(db: Session, project_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """Project에서 Part 배치 해제 — 삭제 건수 반환."""
    return repo.unlink_parts(db, project_id, part_ids)
