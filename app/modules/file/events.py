"""File Aggregate 도메인 이벤트.

Phase 3에서는 이벤트를 발행만 하고, 핸들러는 등록하지 않는다.
"""

from uuid import UUID

from app.core.domain_event import DomainEvent


class FileUploaded(DomainEvent):
    """파일 S3 업로드 확인 완료."""

    file_id: UUID


class FileDeleted(DomainEvent):
    """파일 소프트 삭제."""

    file_id: UUID


class FileExpired(DomainEvent):
    """파일 stale 업로드 만료."""

    file_id: UUID
