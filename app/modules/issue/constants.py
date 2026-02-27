"""이슈 도메인 상수."""

from enum import Enum


class IssueType(str, Enum):
    """이슈 유형."""

    ISSUE = "ISSUE"                    # 일반 이슈
    CHANGE_REQUEST = "CHANGE_REQUEST"  # 변경 요청


class IssueState(str, Enum):
    """이슈 상태."""

    OPEN = "OPEN"      # 열림
    CLOSED = "CLOSED"  # 닫힘


class CRState(str, Enum):
    """변경 요청 상태."""

    DRAFT = "DRAFT"    # 초안
    OPEN = "OPEN"      # 검토 중
    MERGED = "MERGED"  # 반영 완료
    CLOSED = "CLOSED"  # 닫힘
