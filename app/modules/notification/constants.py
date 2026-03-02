"""Notification 도메인 상수."""

from enum import Enum


class NotificationType(str, Enum):
    """알림 유형."""

    MENTION = "MENTION"  # 사용자 멘션
