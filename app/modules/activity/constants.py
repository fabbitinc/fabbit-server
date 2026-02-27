"""Activity 도메인 상수."""

from enum import Enum


class TargetType(str, Enum):
    """활동 기록 대상 유형."""

    PROJECT = "PROJECT"  # 프로젝트 피드
    ISSUE = "ISSUE"      # 이슈 타임라인
