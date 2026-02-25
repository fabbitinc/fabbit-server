"""Mapping Aggregate 도메인 이벤트.

Phase 3에서는 이벤트를 발행만 하고, 핸들러는 등록하지 않는다.
"""

from uuid import UUID

from app.core.domain_event import DomainEvent


class MappingDeactivated(DomainEvent):
    """매핑 비활성화(soft-delete)."""

    mapping_id: UUID
