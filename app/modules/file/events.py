"""File Aggregate 도메인 이벤트."""

from uuid import UUID

from app.core.domain_event import DomainEvent


class FileAttached(DomainEvent):
    """파일을 소유자에 연결."""

    owner_type: str
    owner_id: UUID
    file_ids: list[UUID]


class FileDetached(DomainEvent):
    """파일을 소유자에서 분리."""

    owner_type: str
    owner_id: UUID
    file_id: UUID
