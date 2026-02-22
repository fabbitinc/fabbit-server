"""도면 도메인 상수."""

from enum import Enum


class ConversionStatus(str, Enum):
    """DWG → PDF/썸네일 변환 상태."""

    PENDING = "PENDING"      # 변환 요청됨
    COMPLETED = "COMPLETED"  # 변환 완료
    FAILED = "FAILED"        # 변환 실패


# 도면 등록 허용 확장자 (대소문자 무시)
ALLOWED_DRAWING_EXTENSIONS = {
    ".dwg",   # AutoCAD
    ".dxf",   # AutoCAD 교환 포맷
    ".pdf",   # PDF 도면
    ".png",   # 스캔 이미지
    ".jpg",   # 스캔 이미지
    ".jpeg",  # 스캔 이미지
    ".tif",   # 스캔 이미지
    ".tiff",  # 스캔 이미지
}
