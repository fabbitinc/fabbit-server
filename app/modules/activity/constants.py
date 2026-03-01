"""Activity 도메인 상수."""

from collections import defaultdict
from enum import Enum


class TargetType(str, Enum):
    """활동 기록 대상 유형."""

    PROJECT = "PROJECT"  # 프로젝트 피드
    ISSUE = "ISSUE"      # 이슈 타임라인


class Action(str, Enum):
    """Activity action 열거형. scope를 내장하여 단일 진실 공급원 역할."""

    def __new__(cls, value: str, scope: str) -> "Action":
        obj = str.__new__(cls, value)
        obj._value_ = value
        obj.scope = scope  # type: ignore[attr-defined]
        return obj

    # -- Issue 타임라인 --
    ISSUE_STATE_CHANGED = ("issue_state_changed", "issue")
    ISSUE_TITLE_CHANGED = ("issue_title_changed", "issue")
    CR_STATE_CHANGED = ("cr_state_changed", "cr")
    ASSIGNEE_CHANGED = ("assignee_changed", "assignee")
    REVIEWER_CHANGED = ("reviewer_changed", "reviewer")
    LABEL_CHANGED = ("label_changed", "label")
    PART_CHANGED = ("part_changed", "part")
    FILE_ATTACHED = ("file_attached", "file")
    FILE_DETACHED = ("file_detached", "file")
    CR_ISSUE_LINKED = ("cr_issue_linked", "cr")
    CR_ISSUE_UNLINKED = ("cr_issue_unlinked", "cr")

    # -- Project 피드 --
    ISSUE_CREATED = ("issue_created", "issue")
    ISSUE_CLOSED = ("issue_closed", "issue")
    ISSUE_REOPENED = ("issue_reopened", "issue")
    CR_MERGED = ("cr_merged", "cr")
    PART_ADDED = ("part_added", "part")
    PART_REMOVED = ("part_removed", "part")
    PROJECT_UPDATED = ("project_updated", "project")


class ActivityScope(str, Enum):
    """Activity scope — API query param 타입 검증용."""

    ISSUE = "issue"
    CR = "cr"
    PART = "part"
    ASSIGNEE = "assignee"
    REVIEWER = "reviewer"
    LABEL = "label"
    FILE = "file"
    PROJECT = "project"


# Action enum에서 자동 유도 — 하위호환용 변수명 유지
SCOPE_ACTIONS: dict[ActivityScope, list[Action]] = defaultdict(list)
for _action in Action:
    SCOPE_ACTIONS[ActivityScope(_action.scope)].append(_action)
SCOPE_ACTIONS = dict(SCOPE_ACTIONS)

ACTION_SCOPE: dict[Action, ActivityScope] = {
    action: ActivityScope(action.scope) for action in Action
}
