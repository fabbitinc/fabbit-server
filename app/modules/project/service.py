"""프로젝트(Project) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.project import repository as repo
from app.modules.project.events import ProjectPartsLinked, ProjectPartsUnlinked
from app.modules.project.models import Project


def get_or_raise(db: Session, project_id: uuid.UUID) -> Project:
    """Project 조회 — 없으면 AppError(NOT_FOUND)."""
    project = repo.get_project_by_id(db, project_id)
    if not project:
        raise AppError(message=f"Project '{project_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")
    return project


def create_project(
    db: Session,
    name: str,
    description: str | None = None,
) -> Project:
    """프로젝트 생성."""
    project = Project(name=name, description=description)
    return repo.add(db, project)


def link_parts(
    db: Session, project: Project, part_ids: list[uuid.UUID]
) -> int:
    """Project에 Part 배치 연결 — 신규 연결 건수 반환."""
    count = repo.link_parts(db, project.id, part_ids)
    if count > 0:
        project.register_event(ProjectPartsLinked(
            project_id=project.id, part_ids=part_ids
        ))
    return count


def unlink_parts(
    db: Session, project: Project, part_ids: list[uuid.UUID]
) -> int:
    """Project에서 Part 배치 해제 — 삭제 건수 반환."""
    count = repo.unlink_parts(db, project.id, part_ids)
    if count > 0:
        project.register_event(ProjectPartsUnlinked(
            project_id=project.id, part_ids=part_ids
        ))
    return count
