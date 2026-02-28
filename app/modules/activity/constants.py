"""Activity 도메인 상수."""

from enum import Enum


class TargetType(str, Enum):
    """활동 기록 대상 유형."""

    PROJECT = "PROJECT"  # 프로젝트 피드
    ISSUE = "ISSUE"      # 이슈 타임라인


class Action(str, Enum):
    """Activity action 열거형."""

    # Issue scope
    ISSUE_STATE_CHANGED = "issue_state_changed"
    ISSUE_TITLE_CHANGED = "issue_title_changed"
    ISSUE_CREATED = "issue_created"
    ISSUE_CLOSED = "issue_closed"
    ISSUE_REOPENED = "issue_reopened"

    # CR scope
    CR_STATE_CHANGED = "cr_state_changed"
    CR_MERGED = "cr_merged"
    CR_ISSUE_LINKED = "cr_issue_linked"
    CR_ISSUE_UNLINKED = "cr_issue_unlinked"

    # Part scope
    PART_ADDED = "part_added"
    PART_REMOVED = "part_removed"
    PART_CHANGED = "part_changed"

    # Assignee scope
    ASSIGNEE_CHANGED = "assignee_changed"

    # Label scope
    LABEL_CHANGED = "label_changed"

    # Project scope
    PROJECT_UPDATED = "project_updated"


class ActivityScope(str, Enum):
    """Activity scope — action을 도메인 영역별로 그룹화."""

    ISSUE = "issue"
    CR = "cr"
    PART = "part"
    ASSIGNEE = "assignee"
    LABEL = "label"
    PROJECT = "project"


SCOPE_ACTIONS: dict[ActivityScope, list[Action]] = {
    ActivityScope.ISSUE: [
        Action.ISSUE_STATE_CHANGED,
        Action.ISSUE_TITLE_CHANGED,
        Action.ISSUE_CREATED,
        Action.ISSUE_CLOSED,
        Action.ISSUE_REOPENED,
    ],
    ActivityScope.CR: [
        Action.CR_STATE_CHANGED,
        Action.CR_MERGED,
        Action.CR_ISSUE_LINKED,
        Action.CR_ISSUE_UNLINKED,
    ],
    ActivityScope.PART: [
        Action.PART_ADDED,
        Action.PART_REMOVED,
        Action.PART_CHANGED,
    ],
    ActivityScope.ASSIGNEE: [
        Action.ASSIGNEE_CHANGED,
    ],
    ActivityScope.LABEL: [
        Action.LABEL_CHANGED,
    ],
    ActivityScope.PROJECT: [
        Action.PROJECT_UPDATED,
    ],
}

ACTION_SCOPE: dict[Action, ActivityScope] = {
    action: scope
    for scope, actions in SCOPE_ACTIONS.items()
    for action in actions
}
