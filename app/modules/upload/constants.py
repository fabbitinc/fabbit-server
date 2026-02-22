"""업로드 도메인 상수."""

from enum import Enum


class UploadStatus(str, Enum):
    """업로드 상태."""

    PENDING = "PENDING"      # presigned URL 발급됨, S3 업로드 대기
    UPLOADED = "UPLOADED"    # S3 업로드 확인 완료
    DELETED = "DELETED"      # 소프트 삭제 (보존 기간 후 물리 삭제)
    EXPIRED = "EXPIRED"      # stale 업로드 만료 처리


class ConversionStatus(str, Enum):
    """DWG → PDF/썸네일 변환 상태."""

    PENDING = "PENDING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
