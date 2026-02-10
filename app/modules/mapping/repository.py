"""매핑 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.modules.mapping.models import MappingRecord
from app.modules.upload.models import Upload


def get_upload_by_id(db: Session, upload_id: uuid.UUID) -> Upload | None:
    return db.query(Upload).filter(Upload.id == upload_id).first()


def create_mapping_record(
    db: Session,
    record: MappingRecord,
) -> MappingRecord:
    db.add(record)
    return record


def get_mapping_by_id(db: Session, mapping_id: uuid.UUID) -> MappingRecord | None:
    return db.query(MappingRecord).filter(MappingRecord.id == mapping_id).first()


def list_mappings(db: Session) -> list[MappingRecord]:
    return db.query(MappingRecord).order_by(MappingRecord.created_at.desc()).all()
