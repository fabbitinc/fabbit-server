"""Project 도메인 이벤트."""

from uuid import UUID

from app.core.domain_event import DomainEvent


class ProjectPartsLinked(DomainEvent):
    """프로젝트에 부품 연결 — Project 피드용."""

    project_id: UUID
    part_ids: list[UUID]


class ProjectPartsUnlinked(DomainEvent):
    """프로젝트에서 부품 해제 — Project 피드용."""

    project_id: UUID
    part_ids: list[UUID]
