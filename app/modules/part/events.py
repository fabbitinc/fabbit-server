"""Part Aggregate 도메인 이벤트.

Phase 2에서는 이벤트를 발행만 하고, 핸들러는 등록하지 않는다.
(구독은 Phase 3에서 다른 Aggregate 전환 시 추가)
"""

from uuid import UUID

from app.core.domain_event import DomainEvent


class PartCreated(DomainEvent):
    """Part 신규 생성."""

    part_id: UUID
    part_number: str


class PartPropertiesUpdated(DomainEvent):
    """Part 표준/확장 속성 변경."""

    part_id: UUID
    part_number: str
    changed_fields: list[str]  # 변경된 속성명 목록


class PartDrawingLinked(DomainEvent):
    """Part에 Drawing 연결."""

    part_id: UUID
    drawing_id: UUID


class PartDrawingUnlinked(DomainEvent):
    """Part에서 Drawing 연결 해제."""

    part_id: UUID


class PartFileAttached(DomainEvent):
    """Part에 파일 첨부."""

    part_id: UUID
    file_ids: list[UUID]


class PartFileDetached(DomainEvent):
    """Part에서 파일 분리(소프트 삭제)."""

    part_id: UUID
    file_id: UUID
