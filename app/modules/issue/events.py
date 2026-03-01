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
    """담당자 동기화 — 스냅샷 포함."""

    issue_id: UUID
    added: list[dict]    # [{"user_id": str, "name": str}]
    removed: list[dict]  # [{"user_id": str, "name": str}]


class ReviewersChanged(DomainEvent):
    """검토자 동기화 — 스냅샷 포함."""

    issue_id: UUID
    added: list[dict]    # [{"user_id": str, "name": str}]
    removed: list[dict]  # [{"user_id": str, "name": str}]


class IssueLabelsChanged(DomainEvent):
    """이슈 라벨 동기화 — 스냅샷 포함."""

    issue_id: UUID
    added: list[dict]    # [{"label_id": str, "name": str, "color": str}]
    removed: list[dict]  # [{"label_id": str, "name": str, "color": str}]


class IssuePartsChanged(DomainEvent):
    """이슈 부품 동기화 — 스냅샷 포함."""

    issue_id: UUID
    added: list[dict]    # [{"part_id": str, "part_number": str}]
    removed: list[dict]  # [{"part_id": str, "part_number": str}]


class IssueFilesAttached(DomainEvent):
    """이슈에 파일 첨부 — 스냅샷 포함."""

    issue_id: UUID
    files: list[dict]  # [{"file_id": str, "original_name": str}]


class IssueFileDetached(DomainEvent):
    """이슈에서 파일 분리 — 스냅샷 포함."""

    issue_id: UUID
    file_id: UUID
    file_name: str


class CRIssuesLinked(DomainEvent):
    """변경 요청에 이슈 연결 — 스냅샷 포함."""

    issue_id: UUID
    cr_number: int
    cr_title: str
    linked_issues: list[dict]  # [{"issue_id": str, "number": int, "title": str, "type": str}]


class CRIssuesUnlinked(DomainEvent):
    """변경 요청에서 이슈 해제 — 스냅샷 포함."""

    issue_id: UUID
    cr_number: int
    cr_title: str
    unlinked_issues: list[dict]  # [{"issue_id": str, "number": int, "title": str, "type": str}]


class IssueMentioned(DomainEvent):
    """본문/댓글에서 다른 이슈가 멘션됨."""

    project_id: UUID
    target_issue_id: UUID      # 멘션된 이슈 (Activity 대상)
    source_issue_id: UUID      # 멘션이 작성된 이슈/CR
    source_number: int
    source_title: str
    source_issue_type: str     # "issue" | "change_request"
    is_comment: bool


class UserMentioned(DomainEvent):
    """본문/댓글에서 사용자가 멘션됨 — 향후 notification 모듈에서 구독."""

    project_id: UUID
    mentioned_user_id: UUID
    source_issue_id: UUID
    source_number: int
    source_title: str
    source_issue_type: str     # "issue" | "change_request"
    is_comment: bool
