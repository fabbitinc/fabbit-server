"""구독 관련 상수."""

from enum import Enum


class SubscriptionStatus(str, Enum):
    """구독 상태."""

    ACTIVE = "ACTIVE"  # 현재 빌링 기간 활성
    PAST_DUE = "PAST_DUE"  # 결제 실패 유예
    CANCELED = "CANCELED"  # 취소 (기간 끝까지 사용 가능)
    EXPIRED = "EXPIRED"  # 기간 종료 (이력)


class BillingCycle(str, Enum):
    """빌링 주기."""

    MONTHLY = "MONTHLY"
    YEARLY = "YEARLY"
