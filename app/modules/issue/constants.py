"""이슈 도메인 상수."""

from enum import Enum


class IssueType(str, Enum):
    """이슈 유형."""

    ISSUE = "ISSUE"                    # 일반 이슈
    CHANGE_REQUEST = "CHANGE_REQUEST"  # 변경 요청


class IssueState(str, Enum):
    """이슈 상태.

    상태 전이::

        OPEN ←→ CLOSED

    수정 규칙:
        - OPEN: 수정 가능
        - CLOSED: 수정 불가 (다시 열고 수정)
    """

    OPEN = "OPEN"      # 열림
    CLOSED = "CLOSED"  # 닫힘


class CRState(str, Enum):
    """변경 요청 상태.

    상태 전이::

        DRAFT → SUBMITTED → MERGED (불변)
          │         │
          └→ CLOSED ←┘
               │
               └→ SUBMITTED (reopen)

    수정 규칙:
        - DRAFT, SUBMITTED: 수정 가능
        - MERGED, CLOSED: 수정 불가

    issue.state 동기화:
        - DRAFT, SUBMITTED → issue.state = OPEN
        - MERGED, CLOSED   → issue.state = CLOSED
    """

    DRAFT = "DRAFT"          # 초안
    SUBMITTED = "SUBMITTED"  # 검토 중 (제출됨)
    MERGED = "MERGED"        # 반영 완료
    CLOSED = "CLOSED"        # 닫힘


class ReviewStatus(str, Enum):
    """CR 검토자 리뷰 상태."""

    PENDING = "PENDING"      # 대기
    APPROVED = "APPROVED"    # 승인
    REJECTED = "REJECTED"    # 반려
