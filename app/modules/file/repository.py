"""파일 도메인 데이터 접근 레이어."""

import uuid
from datetime import datetime

from sqlalchemy.orm import Session

from app.modules.file.constants import FileStatus
from app.modules.file.models import File


def create_file_record(
    db: Session,
    file_id: uuid.UUID,
    original_name: str,
    file_key: str,
    content_type: str,
    file_size: int,
    owner_type: str | None,
    owner_id: uuid.UUID | None,
) -> File:
    file = File(
        id=file_id,
        original_name=original_name,
        file_key=file_key,
        content_type=content_type,
        file_size=file_size,
        owner_type=owner_type,
        owner_id=owner_id,
    )
    db.add(file)
    return file


def get_file_by_id(db: Session, file_id: uuid.UUID) -> File | None:
    return db.query(File).filter(File.id == file_id).first()


def get_files_by_ids(db: Session, file_ids: list[uuid.UUID]) -> list[File]:
    return db.query(File).filter(File.id.in_(file_ids)).all()


def get_files_by_owner(
    db: Session,
    owner_type: str,
    owner_id: uuid.UUID,
) -> list[File]:
    """소유자별 파일 목록 조회."""
    return (
        db.query(File)
        .filter(File.owner_type == owner_type, File.owner_id == owner_id)
        .all()
    )


def delete_files_by_owner(
    db: Session,
    owner_type: str,
    owner_id: uuid.UUID,
) -> int:
    """소유자별 파일 레코드 일괄 삭제. 삭제된 행 수를 반환."""
    count = (
        db.query(File)
        .filter(File.owner_type == owner_type, File.owner_id == owner_id)
        .delete()
    )
    return count


def get_stale_pending_files(
    db: Session,
    cutoff: datetime,
    limit: int = 100,
    cursor: uuid.UUID | None = None,
) -> list[File]:
    """PENDING 상태로 cutoff 이전에 생성된 stale 파일 조회."""
    q = db.query(File).filter(
        File.status == FileStatus.PENDING,
        File.created_at < cutoff,
    )
    if cursor:
        q = q.filter(File.id > cursor)
    return q.order_by(File.id).limit(limit).all()


def get_expired_deleted_files(
    db: Session,
    cutoff: datetime,
    limit: int = 100,
    cursor: uuid.UUID | None = None,
) -> list[File]:
    """soft-deleted 후 보존 기간(cutoff)이 만료된 파일 조회."""
    q = (
        db.query(File)
        .execution_options(include_deleted=True)
        .filter(File.deleted_at.isnot(None), File.deleted_at < cutoff)
    )
    if cursor:
        q = q.filter(File.id > cursor)
    return q.order_by(File.id).limit(limit).all()


def get_all_file_keys(db: Session) -> set[str]:
    """DB에 등록된 모든 S3 키 집합 반환.

    PDF/썸네일은 독립 File 레코드로 관리되므로 file_key만 조회.
    """
    rows = (
        db.query(File.file_key)
        .execution_options(include_deleted=True)
        .filter(File.file_key.isnot(None))
        .all()
    )
    return {row[0] for row in rows}
