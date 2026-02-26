"""File 도메인 응답 변환 매퍼."""

from app.infrastructure.s3_client import s3_client
from app.modules.file.models import File
from app.modules.file.schemas import FileItem

_s3 = s3_client


def get_file_url(file_key: str | None) -> str | None:
    """S3 file_key를 공개 접근 URL로 변환 (None-safe)."""
    if not file_key:
        return None
    return _s3.get_file_url(file_key)


def to_file_item(f: File) -> FileItem:
    """File 모델 → 프론트 응답용 FileItem 변환."""
    return FileItem(
        file_id=f.id,
        original_name=f.original_name,
        content_type=f.content_type,
        file_size=f.file_size,
        file_url=get_file_url(f.file_key),
        created_at=f.created_at,
    )


def to_file_items(files: list[File]) -> list[FileItem]:
    """File 목록 → FileItem 목록 변환."""
    return [to_file_item(f) for f in files]
