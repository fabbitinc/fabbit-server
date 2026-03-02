"""라벨(Label) 도메인 Repository."""

import uuid

from sqlalchemy.orm import Session

from app.modules.label.models import Label


def get_by_id(db: Session, label_id: uuid.UUID) -> Label | None:
    """Label 단건 조회."""
    return db.query(Label).filter(Label.id == label_id).first()


def get_by_name(db: Session, name: str) -> Label | None:
    """이름으로 Label 조회."""
    return db.query(Label).filter(Label.name == name).first()


def list_all(db: Session) -> list[Label]:
    """테넌트 전체 라벨 목록 조회."""
    return db.query(Label).order_by(Label.name).all()


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
