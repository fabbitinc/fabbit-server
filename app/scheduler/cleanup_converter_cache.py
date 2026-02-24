"""변환 캐시 정리 job.

만료된 도면 변환 임시 파일을 주기적으로 삭제합니다.
"""

from app.infrastructure.drawing_converter import cleanup_expired_cache


def job() -> None:
    """1시간 경과한 변환 캐시 디렉토리 삭제."""
    cleanup_expired_cache(max_age_hours=1)
