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
    project_id: uuid.UUID | None,
) -> Upload:
    upload = Upload(
        id=upload_id,
        original_name=original_name,
        file_key=file_key,
        content_type=content_type,
        file_size=file_size,
        project_id=project_id,
    )
    db.add(upload)
    return upload


def get_upload_by_id(db: Session, upload_id: uuid.UUID) -> Upload | None:
    return db.query(Upload).filter(Upload.id == upload_id).first()


def get_uploads_by_ids(db: Session, upload_ids: list[uuid.UUID]) -> list[Upload]:
    return db.query(Upload).filter(Upload.id.in_(upload_ids)).all()
