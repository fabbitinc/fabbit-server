"""라벨(Label) 도메인 Repository."""

import uuid

from sqlalchemy.orm import Session

from app.modules.label.models import Label


def get_by_id(db: Session, label_id: uuid.UUID) -> Label | None:
    """Label 단건 조회."""
    return db.query(Label).filter(Label.id == label_id).first()


def get_by_project_and_name(
    db: Session, project_id: uuid.UUID, name: str
) -> Label | None:
    """프로젝트 내 이름으로 Label 조회."""
    return (
        db.query(Label)
        .filter(Label.project_id == project_id, Label.name == name)
        .first()
    )


def list_by_project(db: Session, project_id: uuid.UUID) -> list[Label]:
    """프로젝트의 전체 라벨 목록 조회."""
    return (
        db.query(Label)
        .filter(Label.project_id == project_id)
        .order_by(Label.name)
        .all()
    )


def add(db: Session, entity: Label) -> Label:
    """Label 저장."""
    db.add(entity)
    db.flush()
    return entity


def add_all(db: Session, entities: list[Label]) -> list[Label]:
    """Label 배치 저장."""
    db.add_all(entities)
    db.flush()
    return entities


def delete(db: Session, entity: Label) -> None:
    """Label 삭제."""
    db.delete(entity)
    db.flush()
