"""Activity 도메인 상수."""

from enum import Enum


class TargetType(str, Enum):
    """활동 기록 대상 유형."""

    PROJECT = "PROJECT"
    ISSUE = "ISSUE"
    ORGANIZATION = "ORGANIZATION"


class Action(str, Enum):
    """Activity action 열거형. '{resource}:{verb}' 형식."""

    # -- Issue 타임라인 --
    ISSUE_STATE_CHANGED = "issue:state_changed"
    ISSUE_TITLE_CHANGED = "issue:title_changed"
    CR_STATE_CHANGED = "cr:state_changed"
    ASSIGNEE_CHANGED = "issue:assignee_changed"
    REVIEWER_CHANGED = "issue:reviewer_changed"
    LABEL_CHANGED = "issue:label_changed"
    PART_CHANGED = "issue:part_changed"
    FILE_ATTACHED = "issue:file_attached"
    FILE_DETACHED = "issue:file_detached"
    CR_ISSUE_LINKED = "cr:issue_linked"
    CR_ISSUE_UNLINKED = "cr:issue_unlinked"
    ISSUE_MENTIONED = "issue:mentioned"

    # -- Project 피드 --
    ISSUE_CREATED = "issue:created"
    CR_CREATED = "cr:created"
    ISSUE_CLOSED = "issue:closed"
    ISSUE_REOPENED = "issue:reopened"
    CR_MERGED = "cr:merged"
    PART_ADDED = "project:part_added"
    PART_REMOVED = "project:part_removed"
    PROJECT_UPDATED = "project:updated"
    PROJECT_ARCHIVED = "project:archived"
    PROJECT_UNARCHIVED = "project:unarchived"


def get_scope(action: str) -> str:
    """action 문자열에서 scope(리소스)를 추출. 예: 'issue:created' → 'issue'."""
    return action.split(":")[0]
