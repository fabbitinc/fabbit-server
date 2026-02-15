"""업로드 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.modules.upload.models import Upload


def create_upload_record(
    db: Session,
    upload_id: uuid.UUID,
    original_name: str,
    file_key: str,
    content_type: str,
    file_size: int,
    owner_type: str | None,
    owner_id: uuid.UUID | None,
) -> Upload:
    upload = Upload(
        id=upload_id,
        original_name=original_name,
        file_key=file_key,
        content_type=content_type,
        file_size=file_size,
        owner_type=owner_type,
        owner_id=owner_id,
    )
    db.add(upload)
    return upload


def get_upload_by_id(db: Session, upload_id: uuid.UUID) -> Upload | None:
    return db.query(Upload).filter(Upload.id == upload_id).first()


def get_uploads_by_ids(db: Session, upload_ids: list[uuid.UUID]) -> list[Upload]:
    return db.query(Upload).filter(Upload.id.in_(upload_ids)).all()


def get_uploads_by_owner(
    db: Session,
    owner_type: str,
    owner_id: uuid.UUID,
) -> list[Upload]:
    """소유자별 업로드 파일 목록 조회."""
    return (
        db.query(Upload)
        .filter(Upload.owner_type == owner_type, Upload.owner_id == owner_id)
        .all()
    )


def delete_uploads_by_owner(
    db: Session,
    owner_type: str,
    owner_id: uuid.UUID,
) -> int:
    """소유자별 업로드 레코드 일괄 삭제. 삭제된 행 수를 반환."""
    count = (
        db.query(Upload)
        .filter(Upload.owner_type == owner_type, Upload.owner_id == owner_id)
        .delete()
    )
    return count
