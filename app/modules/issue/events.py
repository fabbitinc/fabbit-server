"""Issue 도메인 이벤트."""

from uuid import UUID

from app.core.domain_event import DomainEvent


class IssueCreated(DomainEvent):
    """일반 이슈 생성 — Project 피드용."""

    project_id: UUID
    issue_id: UUID
    number: int
    title: str


class CRCreated(DomainEvent):
    """변경 요청 생성 — Project 피드용."""

    project_id: UUID
    issue_id: UUID
    number: int
    title: str


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


class AssigneesChanged(DomainEvent):
    """담당자 동기화 — 추가/제거를 한 번에 기록."""

    issue_id: UUID
    added_user_ids: list[UUID]
    removed_user_ids: list[UUID]


class ReviewersChanged(DomainEvent):
    """검토자 동기화 — 추가/제거를 한 번에 기록."""

    issue_id: UUID
    added_user_ids: list[UUID]
    removed_user_ids: list[UUID]


class IssueLabelsChanged(DomainEvent):
    """이슈 라벨 동기화 — 추가/제거를 한 번에 기록."""

    issue_id: UUID
    added_label_ids: list[UUID]
    removed_label_ids: list[UUID]


class IssuePartsChanged(DomainEvent):
    """이슈 부품 동기화 — 추가/제거를 한 번에 기록."""

    issue_id: UUID
    added_part_ids: list[UUID]
    removed_part_ids: list[UUID]


class IssueFilesAttached(DomainEvent):
    """이슈에 파일 첨부."""

    issue_id: UUID
    file_ids: list[UUID]


class IssueFileDetached(DomainEvent):
    """이슈에서 파일 분리."""

    issue_id: UUID
    file_id: UUID


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
