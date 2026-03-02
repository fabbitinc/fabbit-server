"""부품(Part) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid
from typing import TYPE_CHECKING

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.part import repository as repo
from app.modules.part.models import Part

if TYPE_CHECKING:
    from app.modules.file.models import File


def get_or_raise(db: Session, part_id: uuid.UUID) -> Part:
    """Part 조회 — 없으면 AppError(NOT_FOUND)."""
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")
    return part


def attach_files(db: Session, part_id: uuid.UUID, files: list["File"]) -> None:
    """Part에 검증된 파일들을 연결."""
    part = get_or_raise(db, part_id)
    part.attach_files(files)


def detach_file(db: Session, part_id: uuid.UUID, file_id: uuid.UUID) -> None:
    """Part 첨부파일 1건 분리."""
    part = get_or_raise(db, part_id)
    part.detach_file(file_id)


def assign_drawing(db: Session, part_id: uuid.UUID, drawing_id: uuid.UUID) -> None:
    """Part에 Drawing 연결."""
    part = get_or_raise(db, part_id)
    part.assign_drawing(drawing_id)


def unassign_drawing(db: Session, part_id: uuid.UUID) -> uuid.UUID:
    """Part에서 Drawing 연결 해제 — drawing_id 반환."""
    part = get_or_raise(db, part_id)
    if part.drawing_id is None:
        raise AppError(message="연결된 도면이 없습니다", code="NOT_FOUND")
    drawing_id = part.drawing_id
    part.unassign_drawing()
    return drawing_id


# ── 담당자 / 담당팀 ──


def add_assignees(
    db: Session,
    part_id: uuid.UUID,
    assignments: list[dict],
) -> int:
    """Part에 담당자 배치 추가 — 신규 추가 건수 반환."""
    return repo.add_assignees(db, part_id, assignments)


def remove_assignees(
    db: Session,
    part_id: uuid.UUID,
    assignments: list[dict],
) -> int:
    """Part에서 담당자 배치 제거 — 삭제 건수 반환."""
    return repo.remove_assignees(db, part_id, assignments)


def add_team_assignments(
    db: Session,
    part_id: uuid.UUID,
    assignments: list[dict],
) -> int:
    """Part에 담당팀 배치 추가 — 신규 추가 건수 반환."""
    return repo.add_team_assignments(db, part_id, assignments)


def remove_team_assignments(
    db: Session,
    part_id: uuid.UUID,
    assignments: list[dict],
) -> int:
    """Part에서 담당팀 배치 제거 — 삭제 건수 반환."""
    return repo.remove_team_assignments(db, part_id, assignments)
