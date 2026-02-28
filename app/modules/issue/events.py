"""Issue 도메인 이벤트."""

from uuid import UUID

from app.core.domain_event import DomainEvent


class IssueCreated(DomainEvent):
    """이슈/변경요청 생성 — Project 피드용."""

    project_id: UUID
    issue_id: UUID
    number: int
    title: str
    issue_type: str


class IssueStateChanged(DomainEvent):
    """이슈 상태 변경 (OPEN ↔ CLOSED) — 양쪽 피드."""

    project_id: UUID
    issue_id: UUID
    number: int
    title: str
    old_state: str
    new_state: str


class CRStateChanged(DomainEvent):
    """변경요청 상태 변경 — 양쪽 피드 (MERGED만 Project)."""

    project_id: UUID
    issue_id: UUID
    number: int
    title: str
    old_state: str
    new_state: str


class AssigneesAdded(DomainEvent):
    """담당자 배정 — Issue 피드용."""

    issue_id: UUID
    user_ids: list[UUID]


class AssigneesRemoved(DomainEvent):
    """담당자 해제 — Issue 피드용."""

    issue_id: UUID
    user_ids: list[UUID]


class IssueLabelsChanged(DomainEvent):
    """이슈 라벨 동기화 — 추가/제거를 한 번에 기록."""

    issue_id: UUID
    added_label_ids: list[UUID]
    removed_label_ids: list[UUID]


class IssuePartsLinked(DomainEvent):
    """이슈에 부품 연결 — Issue 피드용."""

    issue_id: UUID
    part_ids: list[UUID]


class IssuePartsUnlinked(DomainEvent):
    """이슈에서 부품 해제 — Issue 피드용."""

    issue_id: UUID
    part_ids: list[UUID]


class CRIssuesLinked(DomainEvent):
    """변경 요청에 이슈 연결 — CR·이슈 양쪽 피드."""

    issue_id: UUID
    cr_number: int
    cr_title: str
    linked_issue_ids: list[UUID]


class CRIssuesUnlinked(DomainEvent):
    """변경 요청에서 이슈 해제 — CR·이슈 양쪽 피드."""

    issue_id: UUID
    cr_number: int
    cr_title: str
    unlinked_issue_ids: list[UUID]
