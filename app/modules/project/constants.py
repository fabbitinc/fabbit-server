"""프로젝트 관련 상수."""

from enum import Enum


class ProjectRole(str, Enum):
    """프로젝트 멤버 역할."""

    ADMIN = "ADMIN"      # 관리자 (설정 변경, 멤버 관리)
    MEMBER = "MEMBER"    # 일반 멤버 (읽기/쓰기)
    VIEWER = "VIEWER"    # 뷰어 (읽기 전용)
