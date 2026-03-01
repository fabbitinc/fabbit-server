"""infrastructure 공용 유틸리티."""

from pathlib import PurePosixPath


def replace_suffix(file_key: str, suffix: str) -> str:
    """S3 file_key의 확장자를 교체.

    예: tenants/.../photo.png → tenants/.../photo.webp
    """
    return str(PurePosixPath(file_key).with_suffix(suffix))
