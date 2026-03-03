"""부품(Part) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid
from typing import TYPE_CHECKING

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.part import repository as repo
from app.modules.part.models import CategoryDefaultAssignment, Part

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


# ── 카테고리별 기본 담당자/팀 ──


def upsert_category_default(
    db: Session,
    category: str | None,
    owner_id: uuid.UUID | None,
    owner_team_id: uuid.UUID | None,
) -> CategoryDefaultAssignment:
    """카테고리별 기본 담당자/팀 설정 upsert."""
    return repo.upsert_category_default(db, category, owner_id, owner_team_id)


def delete_category_default(db: Session, category: str | None) -> None:
    """카테고리별 기본 담당자/팀 설정 삭제 — 없으면 AppError."""
    deleted = repo.delete_category_default(db, category)
    if not deleted:
        raise AppError(
            message=f"카테고리 '{category}' 기본값 설정을 찾을 수 없습니다",
            code="NOT_FOUND",
        )
