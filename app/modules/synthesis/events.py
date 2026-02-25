"""SynthesisJob Aggregate 도메인 이벤트.

Phase 3에서는 이벤트를 발행만 하고, 핸들러는 등록하지 않는다.
"""

from uuid import UUID

from app.core.domain_event import DomainEvent


class SynthesisJobStarted(DomainEvent):
    """합성 작업 시작."""

    job_id: UUID


class SynthesisJobCompleted(DomainEvent):
    """합성 작업 완료."""

    job_id: UUID
    nodes_created: int
    relationships_created: int


class SynthesisJobFailed(DomainEvent):
    """합성 작업 실패."""

    job_id: UUID
    errors: list[str]
