"""매핑 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.modules.mapping.models import MappingRecord, MappingRevision
from app.modules.file.models import File


def get_file_by_id(db: Session, file_id: uuid.UUID) -> File | None:
    return db.query(File).filter(File.id == file_id).first()


def exists_by_name(
    db: Session, name: str, exclude_id: uuid.UUID | None = None
) -> bool:
    """이름 중복 여부 확인 (비활성 포함)."""
    query = db.query(MappingRecord).filter(MappingRecord.name == name)
    if exclude_id is not None:
        query = query.filter(MappingRecord.id != exclude_id)
    return query.first() is not None


def create_mapping_record(
    db: Session,
    record: MappingRecord,
    revision: MappingRevision,
) -> MappingRecord:
    db.add(record)
    db.add(revision)
    return record


def create_revision(db: Session, revision: MappingRevision) -> MappingRevision:
    db.add(revision)
    return revision


def get_mapping_by_id(
    db: Session, mapping_id: uuid.UUID
) -> tuple[MappingRecord, MappingRevision] | None:
    """Record + 최신 Revision 반환."""
    record = db.query(MappingRecord).filter(MappingRecord.id == mapping_id).first()
    if record is None:
        return None
    revision = get_latest_revision(db, record.id)
    if revision is None:
        return None
    return record, revision


def get_latest_revision(
    db: Session, record_id: uuid.UUID
) -> MappingRevision | None:
    return (
        db.query(MappingRevision)
        .filter(MappingRevision.record_id == record_id)
        .order_by(MappingRevision.version.desc())
        .first()
    )


def list_mappings(db: Session) -> list[tuple[MappingRecord, MappingRevision]]:
    """is_active=True인 Record + 최신 Revision 목록."""
    records = (
        db.query(MappingRecord)
        .filter(MappingRecord.is_active.is_(True))
        .order_by(MappingRecord.created_at.desc())
        .all()
    )
    result: list[tuple[MappingRecord, MappingRevision]] = []
    for record in records:
        revision = get_latest_revision(db, record.id)
        if revision is not None:
            result.append((record, revision))
    return result
