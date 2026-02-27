"""Activity 이벤트 핸들러.

다른 Aggregate에서 발생한 이벤트를 구독하여 Activity 레코드를 생성한다.
같은 트랜잭션 내에서 실행되므로 비즈니스 로직과 함께 commit/rollback 된다.
"""

from app.core.event_bus import event_bus
from app.core.transactional import get_active_session
from app.modules.activity.constants import TargetType
from app.modules.activity.models import Activity
from app.modules.issue.events import (
    AssigneesAdded,
    AssigneesRemoved,
    CRStateChanged,
    IssueCreated,
    IssuePartsLinked,
    IssuePartsUnlinked,
    IssueStateChanged,
)
from app.modules.project.events import ProjectPartsLinked, ProjectPartsUnlinked


def _get_actor_id():
    """현재 세션의 user_id 획득."""
    db = get_active_session()
    return db.info.get("user_id")


def _add_activity(target_type, target_id, action, actor_id, detail=None):
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


def _on_issue_created(event: IssueCreated) -> None:
    """이슈/CR 생성 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        "issue_created",
        actor_id,
        {
            "issue_id": str(event.issue_id),
            "number": event.number,
            "title": event.title,
            "type": event.issue_type,
        },
    )


def _on_issue_state_changed(event: IssueStateChanged) -> None:
    """이슈 상태 변경 → 양쪽 피드."""
    actor_id = _get_actor_id()
    # Issue 피드
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        "state_changed",
        actor_id,
        {"from": event.old_state, "to": event.new_state},
    )
    # Project 피드
    if event.new_state == "CLOSED":
        action = "issue_closed"
    else:
        action = "issue_reopened"
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        action,
        actor_id,
        {
            "issue_id": str(event.issue_id),
            "number": event.number,
            "title": event.title,
        },
    )


def _on_cr_state_changed(event: CRStateChanged) -> None:
    """CR 상태 변경 → Issue 피드 + (MERGED만) Project 피드."""
    actor_id = _get_actor_id()
    # Issue 피드
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        "cr_state_changed",
        actor_id,
        {"from": event.old_state, "to": event.new_state},
    )
    # MERGED만 Project 피드
    if event.new_state == "MERGED":
        _add_activity(
            TargetType.PROJECT,
            event.project_id,
            "cr_merged",
            actor_id,
            {
                "issue_id": str(event.issue_id),
                "number": event.number,
                "title": event.title,
            },
        )


def _on_assignees_added(event: AssigneesAdded) -> None:
    """담당자 배정 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        "assignee_added",
        actor_id,
        {"user_ids": [str(uid) for uid in event.user_ids]},
    )


def _on_assignees_removed(event: AssigneesRemoved) -> None:
    """담당자 해제 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        "assignee_removed",
        actor_id,
        {"user_ids": [str(uid) for uid in event.user_ids]},
    )


def _on_issue_parts_linked(event: IssuePartsLinked) -> None:
    """이슈에 부품 연결 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        "part_added",
        actor_id,
        {"part_ids": [str(pid) for pid in event.part_ids]},
    )


def _on_issue_parts_unlinked(event: IssuePartsUnlinked) -> None:
    """이슈에서 부품 해제 → Issue 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.ISSUE,
        event.issue_id,
        "part_removed",
        actor_id,
        {"part_ids": [str(pid) for pid in event.part_ids]},
    )


# ── Project 이벤트 ──


def _on_project_parts_linked(event: ProjectPartsLinked) -> None:
    """프로젝트에 부품 연결 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        "part_added",
        actor_id,
        {"part_ids": [str(pid) for pid in event.part_ids]},
    )


def _on_project_parts_unlinked(event: ProjectPartsUnlinked) -> None:
    """프로젝트에서 부품 해제 → Project 피드."""
    actor_id = _get_actor_id()
    _add_activity(
        TargetType.PROJECT,
        event.project_id,
        "part_removed",
        actor_id,
        {"part_ids": [str(pid) for pid in event.part_ids]},
    )


# ── 구독 등록 ──

event_bus.subscribe(IssueCreated, _on_issue_created)
event_bus.subscribe(IssueStateChanged, _on_issue_state_changed)
event_bus.subscribe(CRStateChanged, _on_cr_state_changed)
event_bus.subscribe(AssigneesAdded, _on_assignees_added)
event_bus.subscribe(AssigneesRemoved, _on_assignees_removed)
event_bus.subscribe(IssuePartsLinked, _on_issue_parts_linked)
event_bus.subscribe(IssuePartsUnlinked, _on_issue_parts_unlinked)
event_bus.subscribe(ProjectPartsLinked, _on_project_parts_linked)
event_bus.subscribe(ProjectPartsUnlinked, _on_project_parts_unlinked)
