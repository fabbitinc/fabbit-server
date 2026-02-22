"""파일 도메인 데이터 접근 레이어."""

import uuid
from datetime import datetime

from sqlalchemy import union_all
from sqlalchemy.orm import Session

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


def get_stale_files(
    db: Session,
    status: str,
    cutoff: datetime,
    limit: int = 100,
    cursor: uuid.UUID | None = None,
) -> list[File]:
    """정리 대상 파일 조회 (cursor 기반 페이지네이션).

    - status="PENDING", cutoff 이전 → stale
    - status="DELETED", cutoff 이전 → soft delete 만료
    """
    q = db.query(File).filter(
        File.status == status,
        File.created_at < cutoff,
    )
    if cursor:
        q = q.filter(File.id > cursor)
    return q.order_by(File.id).limit(limit).all()


def get_all_file_keys(db: Session) -> set[str]:
    """DB에 등록된 모든 S3 키(file_key, pdf_key, thumbnail_key) 집합 반환."""
    q = union_all(
        db.query(File.file_key).filter(File.file_key.isnot(None)),
        db.query(File.pdf_key).filter(File.pdf_key.isnot(None)),
        db.query(File.thumbnail_key).filter(File.thumbnail_key.isnot(None)),
    )
    rows = db.execute(q).all()
    return {row[0] for row in rows}
