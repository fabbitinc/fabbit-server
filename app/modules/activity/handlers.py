"""Activity 이벤트 핸들러.

다른 Aggregate에서 발생한 이벤트를 구독하여 Activity 레코드를 생성한다.
같은 트랜잭션 내에서 실행되므로 비즈니스 로직과 함께 commit/rollback 된다.
"""

from uuid import UUID

from app.core.event_bus import event_bus
from app.core.transactional import get_active_session
from app.modules.activity.constants import Action, TargetType
from app.modules.activity.models import Activity
from app.modules.issue.events import (
    AssigneesChanged,
    CRIssuesLinked,
    CRIssuesUnlinked,
    CRStateChanged,
    IssueFileDetached,
    IssueFilesAttached,
    IssueLabelsChanged,
    IssueMentioned,
    IssuePartsChanged,
    IssueStateChanged,
    ReviewersChanged,
)
from app.modules.project.events import (
    ProjectArchived,
    ProjectPartsLinked,
    ProjectPartsUnlinked,
    ProjectUnarchived,
    ProjectUpdated,
)


def _get_actor_id():
    """현재 세션의 user_id 획득."""
    db = get_active_session()
    return db.info.get("user_id")


def _add_activity(target_type, target_id, action: Action, actor_id, detail=None):
    """Activity 레코드 생성 헬퍼."""
    db = get_active_session()
    activity = Activity(
        target_type=target_type,
        target_id=target_id,
        action=action,
        actor_id=actor_id,
        detail=detail,
    )
    db.add(activity)


# ── Issue 이벤트 ──


def _on_issue_state_changed(event: IssueStateChanged) -> None:
    """이슈 상태 변경 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.ISSUE_STATE_CHANGED,
        actor_id,
        {"from": event.old_state, "to": event.new_state},
    )


def _on_cr_state_changed(event: CRStateChanged) -> None:
    """CR 상태 변경 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.CR_STATE_CHANGED,
        actor_id,
        {"from": event.old_state, "to": event.new_state},
    )


def _on_assignees_changed(event: AssigneesChanged) -> None:
    """담당자 동기화 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.ASSIGNEE_CHANGED,
        actor_id,
        {"added": event.added, "removed": event.removed},
    )


def _on_reviewers_changed(event: ReviewersChanged) -> None:
    """검토자 동기화 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.REVIEWER_CHANGED,
        actor_id,
        {"added": event.added, "removed": event.removed},
    )


def _on_issue_labels_changed(event: IssueLabelsChanged) -> None:
    """이슈 라벨 변경 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.LABEL_CHANGED,
        actor_id,
        {"added": event.added, "removed": event.removed},
    )


def _on_issue_parts_changed(event: IssuePartsChanged) -> None:
    """이슈 부품 동기화 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.PART_CHANGED,
        actor_id,
        {"added": event.added, "removed": event.removed},
    )


def _on_issue_files_attached(event: IssueFilesAttached) -> None:
    """이슈에 파일 첨부 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.FILE_ATTACHED,
        actor_id,
        {"files": event.files},
    )


def _on_issue_file_detached(event: IssueFileDetached) -> None:
    """이슈에서 파일 분리 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.FILE_DETACHED,
        actor_id,
        {"file_id": str(event.file_id), "file_name": event.file_name},
    )


def _on_cr_issues_linked(event: CRIssuesLinked) -> None:
    """CR에 이슈 연결 → CR 피드 + 각 이슈 피드."""
    actor_id = _get_actor_id()
    # CR 피드
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.CR_ISSUE_LINKED,
        actor_id,
        {"linked_issues": event.linked_issues},
    )
    # 연결된 각 이슈 피드
    for issue_info in event.linked_issues:
        _add_activity(
            TargetType.ISSUE,
            UUID(issue_info["issue_id"]),
            Action.CR_ISSUE_LINKED,
            actor_id,
            {
                "cr_id": str(event.issue_id),
                "cr_number": event.cr_number,
                "cr_title": event.cr_title,
            },
        )


def _on_cr_issues_unlinked(event: CRIssuesUnlinked) -> None:
    """CR에서 이슈 해제 → CR 피드 + 각 이슈 피드."""
    actor_id = _get_actor_id()
    # CR 피드
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        Action.CR_ISSUE_UNLINKED,
        actor_id,
        {"unlinked_issues": event.unlinked_issues},
    )
    # 연결 해제된 각 이슈 피드
    for issue_info in event.unlinked_issues:
        _add_activity(
            TargetType.ISSUE,
            UUID(issue_info["issue_id"]),
            Action.CR_ISSUE_UNLINKED,
            actor_id,
            {
                "cr_id": str(event.issue_id),
                "cr_number": event.cr_number,
                "cr_title": event.cr_title,
            },
        )


def _on_issue_mentioned(event: IssueMentioned) -> None:
    """이슈 멘션 → 멘션된 이슈 타임라인에 기록."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.target_issue_id,
        Action.ISSUE_MENTIONED,
        actor_id,
        {
            "source_issue_id": str(event.source_issue_id),
            "source_number": event.source_number,
            "source_title": event.source_title,
            "source_issue_type": event.source_issue_type,
            "is_comment": event.is_comment,
        },
    )


# ── Project 이벤트 ──


def _on_project_parts_linked(event: ProjectPartsLinked) -> None:
    """프로젝트에 부품 연결 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        Action.PART_ADDED,
        actor_id,
        {"parts": event.parts},
    )


def _on_project_parts_unlinked(event: ProjectPartsUnlinked) -> None:
    """프로젝트에서 부품 해제 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        Action.PART_REMOVED,
        actor_id,
        {"parts": event.parts},
    )


def _on_project_updated(event: ProjectUpdated) -> None:
    """프로젝트 정보 수정 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        Action.PROJECT_UPDATED,
        actor_id,
        {"changes": event.changes},
    )


def _on_project_archived(event: ProjectArchived) -> None:
    """프로젝트 보관 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        Action.PROJECT_ARCHIVED,
        actor_id,
    )


def _on_project_unarchived(event: ProjectUnarchived) -> None:
    """프로젝트 보관 해제 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        Action.PROJECT_UNARCHIVED,
        actor_id,
    )


# ── 구독 등록 ──

event_bus.subscribe(IssueStateChanged, _on_issue_state_changed)
event_bus.subscribe(CRStateChanged, _on_cr_state_changed)
event_bus.subscribe(AssigneesChanged, _on_assignees_changed)
event_bus.subscribe(ReviewersChanged, _on_reviewers_changed)
event_bus.subscribe(IssueLabelsChanged, _on_issue_labels_changed)
event_bus.subscribe(IssuePartsChanged, _on_issue_parts_changed)
event_bus.subscribe(IssueFilesAttached, _on_issue_files_attached)
event_bus.subscribe(IssueFileDetached, _on_issue_file_detached)
event_bus.subscribe(CRIssuesLinked, _on_cr_issues_linked)
event_bus.subscribe(CRIssuesUnlinked, _on_cr_issues_unlinked)
event_bus.subscribe(IssueMentioned, _on_issue_mentioned)
event_bus.subscribe(ProjectPartsLinked, _on_project_parts_linked)
event_bus.subscribe(ProjectPartsUnlinked, _on_project_parts_unlinked)
event_bus.subscribe(ProjectUpdated, _on_project_updated)
event_bus.subscribe(ProjectArchived, _on_project_archived)
event_bus.subscribe(ProjectUnarchived, _on_project_unarchived)
