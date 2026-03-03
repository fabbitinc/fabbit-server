"""부품(Part) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid
from typing import TYPE_CHECKING

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.part import repository as repo
from app.modules.part.models import Part, PartDefaultOwner

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


# ── Part 담당자/팀 ──

_SENTINEL = object()


def update_owner(
    db: Session,
    part_id: uuid.UUID,
    owner_id: uuid.UUID | None | object = _SENTINEL,
    owner_team_id: uuid.UUID | None | object = _SENTINEL,
) -> Part:
    """Part 담당자/팀 수정 (PATCH 시맨틱)."""
    part = get_or_raise(db, part_id)
    if owner_id is not _SENTINEL:
        if owner_id is None:
            part.unassign_owner()
        else:
            part.assign_owner(owner_id)  # type: ignore[arg-type]
    if owner_team_id is not _SENTINEL:
        if owner_team_id is None:
            part.unassign_owner_team()
        else:
            part.assign_owner_team(owner_team_id)  # type: ignore[arg-type]
    db.flush()
    return part


# ── 카테고리 ──


def rename_category(db: Session, old_name: str, new_name: str) -> int:
    """카테고리 이름 일괄 변경 — 해당 카테고리 없으면 AppError.

    대상 카테고리(new_name)가 이미 존재하면 병합:
    - 원본 카테고리의 기본 담당자 삭제
    - 원본 Part들의 담당자를 대상 카테고리 기본 담당자로 갱신
      (대상 담당자도 비어있으면 fallback(category=NULL) 참조, 그것도 없으면 NULL)
    """
    if old_name == new_name:
        raise AppError(message="변경 전후 카테고리 이름이 동일합니다", code="BAD_REQUEST")

    existing = repo.get_category_stats(db)
    if not any(cat == old_name for cat, _ in existing):
        raise AppError(
            message=f"카테고리 '{old_name}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    # 병합 여부 판단: 대상 카테고리에 기본 담당자가 존재하는지
    target_owner = repo.get_default_owner(db, new_name)
    is_merge = target_owner is not None and target_owner.category == new_name

    if is_merge:
        # 카테고리 변경 전에 원본 Part ID 수집
        old_part_ids = repo.get_part_ids_by_category(db, old_name)

    # Part.category 일괄 변경
    count = repo.rename_parts_category(db, old_name, new_name)

    if is_merge:
        # 원본 카테고리의 기본 담당자 삭제
        repo.delete_default_owner(db, old_name)

        # 담당자 결정: 대상 카테고리 → fallback(category=NULL) → NULL
        resolved = repo.get_default_owner(db, new_name)
        owner_id = resolved.default_owner_id if resolved else None
        owner_team_id = resolved.default_owner_team_id if resolved else None

        # 원본 Part들의 담당자 갱신
        repo.bulk_update_owner(db, old_part_ids, owner_id, owner_team_id)
    else:
        # 단순 이름 변경
        repo.rename_default_owner_category(db, old_name, new_name)

    return count


# ── 기본 담당자/팀 ──


def upsert_default_owner(
    db: Session,
    category: str | None,
    owner_id: uuid.UUID | None,
    owner_team_id: uuid.UUID | None,
) -> PartDefaultOwner:
    """기본 담당자/팀 설정 upsert."""
    return repo.upsert_default_owner(db, category, owner_id, owner_team_id)


def delete_default_owner(db: Session, category: str | None) -> None:
    """기본 담당자/팀 설정 삭제 — 없으면 AppError."""
    deleted = repo.delete_default_owner(db, category)
    if not deleted:
        raise AppError(
            message=f"카테고리 '{category}' 기본값 설정을 찾을 수 없습니다",
            code="NOT_FOUND",
        )
