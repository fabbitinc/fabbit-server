"""파일 도메인 상수."""

from enum import Enum


class FileStatus(str, Enum):
    """파일 상태."""

    PENDING = "PENDING"      # presigned URL 발급됨, S3 업로드 대기
    UPLOADED = "UPLOADED"    # S3 업로드 확인 완료
