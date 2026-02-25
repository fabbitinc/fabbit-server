"""합성 도메인 상수."""

from enum import Enum


class SynthesisJobStatus(str, Enum):
    """합성 작업 상태."""

    PENDING = "PENDING"        # 생성됨, 실행 대기
    PROCESSING = "PROCESSING"  # 실행 중
    COMPLETED = "COMPLETED"    # 완료
    FAILED = "FAILED"          # 실패
