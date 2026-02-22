"""도면 도메인 상수."""

from enum import Enum


class ConversionStatus(str, Enum):
    """DWG → PDF/썸네일 변환 상태."""

    PENDING = "PENDING"      # 변환 요청됨
    COMPLETED = "COMPLETED"  # 변환 완료
    FAILED = "FAILED"        # 변환 실패
