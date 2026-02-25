"""Drawing Aggregate 도메인 이벤트.

Phase 3에서는 이벤트를 발행만 하고, 핸들러는 등록하지 않는다.
"""

from uuid import UUID

from app.core.domain_event import DomainEvent


class DrawingConversionCompleted(DomainEvent):
    """Drawing DWG→PDF 변환 완료."""

    drawing_id: UUID


class DrawingConversionFailed(DomainEvent):
    """Drawing DWG→PDF 변환 실패."""

    drawing_id: UUID


class DrawingPropertiesUpdated(DomainEvent):
    """Drawing 표준/확장 속성 변경."""

    drawing_id: UUID
    changed_fields: list[str]
