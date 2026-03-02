"""라벨(Label) 쓰기 비즈니스 로직.

트랜잭션은 use_case 레이어에서 관리합니다.
자기 도메인 repo만 호출 — 타 도메인 접근 금지.
"""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.modules.label import repository as repo
from app.modules.label.constants import DEFAULT_LABELS
from app.modules.label.models import Label


def get_or_raise(db: Session, label_id: uuid.UUID) -> Label:
    """Label 조회 — 없으면 AppError(NOT_FOUND)."""
    label = repo.get_by_id(db, label_id)
    if not label:
        raise AppError(
            message=f"Label '{label_id}'을(를) 찾을 수 없습니다",
            code="NOT_FOUND",
        )
    return label


def create_label(
    db: Session,
    name: str,
    color: str,
    description: str | None = None,
) -> Label:
    """라벨 생성 — 이름 중복 검사."""
    existing = repo.get_by_name(db, name)
    if existing:
        raise AppError(
            message=f"동일한 이름의 '{name}' 라벨이 이미 존재합니다",
            code="ALREADY_EXISTS",
        )
    label = Label(
        name=name,
        color=color,
        description=description,
    )
    return repo.add(db, label)


def update_label(
    db: Session,
    label_id: uuid.UUID,
    *,
    name: str | None = None,
    description: str | None = None,
    color: str | None = None,
    _unset_description: bool = False,
) -> Label:
    """라벨 수정."""
    label = get_or_raise(db, label_id)

    if name is not None and name != label.name:
        existing = repo.get_by_name(db, name)
        if existing:
            raise AppError(
                message=f"동일한 이름의 '{name}' 라벨이 이미 존재합니다",
                code="ALREADY_EXISTS",
            )
        label.name = name

    if _unset_description:
        label.description = None
    elif description is not None:
        label.description = description

    if color is not None:
        label.color = color

    db.flush()
    return label


def delete_label(db: Session, label_id: uuid.UUID) -> None:
    """라벨 삭제."""
    label = get_or_raise(db, label_id)
    repo.delete(db, label)


def seed_defaults(db: Session) -> list[Label]:
    """기본 라벨 일괄 생성."""
    labels = [Label(**data) for data in DEFAULT_LABELS]
    return repo.add_all(db, labels)
