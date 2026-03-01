"""프로젝트(Project) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.project import repository as repo
from app.modules.project.events import ProjectPartsLinked, ProjectPartsUnlinked, ProjectUpdated
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


def update_project(
    db: Session,
    project: Project,
    name: str | None = None,
    description: str | None = None,
) -> Project:
    """프로젝트 정보 수정 — 변경된 필드만 감지하여 이벤트 발행."""
    changes: dict = {}
    if name is not None and name != project.name:
        changes["name"] = {"from": project.name, "to": name}
        project.name = name
    if description is not None and description != project.description:
        changes["description"] = {"from": project.description, "to": description}
        project.description = description
    if changes:
        project.register_event(ProjectUpdated(
            project_id=project.id, changes=changes
        ))
    return project


def link_parts(
    db: Session, project: Project, part_ids: list[uuid.UUID]
) -> int:
    """Project에 Part 배치 연결 — 신규 연결 건수 반환."""
    count = repo.link_parts(db, project.id, part_ids)
    if count > 0:
        from app.modules.part.models import Part

        parts_map = {
            p.id: p for p in db.query(Part).filter(Part.id.in_(part_ids)).all()
        }
        project.register_event(ProjectPartsLinked(
            project_id=project.id,
            parts=[
                {"part_id": str(pid), "part_number": parts_map[pid].part_number}
                for pid in part_ids
                if pid in parts_map
            ],
        ))
    return count


def unlink_parts(
    db: Session, project: Project, part_ids: list[uuid.UUID]
) -> int:
    """Project에서 Part 배치 해제 — 삭제 건수 반환."""
    count = repo.unlink_parts(db, project.id, part_ids)
    if count > 0:
        from app.modules.part.models import Part

        parts_map = {
            p.id: p for p in db.query(Part).filter(Part.id.in_(part_ids)).all()
        }
        project.register_event(ProjectPartsUnlinked(
            project_id=project.id,
            parts=[
                {"part_id": str(pid), "part_number": parts_map[pid].part_number}
                for pid in part_ids
                if pid in parts_map
            ],
        ))
    return count


def validate_parts_in_project(
    db: Session, project_id: uuid.UUID, part_ids: list[uuid.UUID]
) -> None:
    """part_ids가 모두 프로젝트에 연결되어 있는지 검증 — 아니면 AppError."""
    invalid = repo.filter_unlinked_part_ids(db, project_id, part_ids)
    if invalid:
        raise AppError(
            message=f"프로젝트에 연결되지 않은 부품입니다: {invalid}",
            code="INVALID_PART",
        )


def add_members(
    db: Session, project_id: uuid.UUID, user_ids: list[uuid.UUID]
) -> int:
    """Project에 멤버 배치 추가 — 신규 추가 건수 반환."""
    return repo.add_members(db, project_id, user_ids)


def remove_members(
    db: Session, project_id: uuid.UUID, user_ids: list[uuid.UUID]
) -> int:
    """Project에서 멤버 배치 제거 — 삭제 건수 반환."""
    return repo.remove_members(db, project_id, user_ids)
