"""Project 도메인 이벤트."""

from uuid import UUID

from app.core.domain_event import DomainEvent


class ProjectPartsLinked(DomainEvent):
    """프로젝트에 부품 연결 — 스냅샷 포함."""

    project_id: UUID
    parts: list[dict]  # [{"part_id": str, "part_number": str}]


class ProjectPartsUnlinked(DomainEvent):
    """프로젝트에서 부품 해제 — 스냅샷 포함."""

    project_id: UUID
    parts: list[dict]  # [{"part_id": str, "part_number": str}]


class ProjectUpdated(DomainEvent):
    """프로젝트 정보 수정 — Project 피드용."""

    project_id: UUID
    changes: dict  # {"name": {"from": "old", "to": "new"}, ...}


class ProjectArchived(DomainEvent):
    """프로젝트 보관."""

    project_id: UUID


class ProjectUnarchived(DomainEvent):
    """프로젝트 보관 해제."""

    project_id: UUID
