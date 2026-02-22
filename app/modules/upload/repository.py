"""업로드 도메인 데이터 접근 레이어."""

import uuid
from datetime import datetime

from sqlalchemy import union_all
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


def get_stale_uploads(
    db: Session,
    status: str,
    cutoff: datetime,
    limit: int = 100,
    cursor: uuid.UUID | None = None,
) -> list[Upload]:
    """정리 대상 업로드 조회 (cursor 기반 페이지네이션).

    - status="PENDING", cutoff 이전 → orphan
    - status="DELETED", cutoff 이전 → soft delete 만료
    """
    q = db.query(Upload).filter(
        Upload.status == status,
        Upload.created_at < cutoff,
    )
    if cursor:
        q = q.filter(Upload.id > cursor)
    return q.order_by(Upload.id).limit(limit).all()


def get_all_file_keys(db: Session) -> set[str]:
    """DB에 등록된 모든 S3 키(file_key, pdf_key, thumbnail_key) 집합 반환."""
    q = union_all(
        db.query(Upload.file_key).filter(Upload.file_key.isnot(None)),
        db.query(Upload.pdf_key).filter(Upload.pdf_key.isnot(None)),
        db.query(Upload.thumbnail_key).filter(Upload.thumbnail_key.isnot(None)),
    )
    rows = db.execute(q).all()
    return {row[0] for row in rows}
