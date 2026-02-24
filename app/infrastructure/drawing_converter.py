"""도면 변환 — QCAD CLI + PyMuPDF 기반 로컬 변환.

S3에서 원본 파일을 다운로드하여 PDF + 썸네일을 생성하고,
결과를 S3에 업로드합니다. BackgroundTask에서 동기적으로 실행됩니다.

캐시 구조 — S3 key를 그대로 로컬 경로로 미러링:
  /tmp/drawing-converter/
    tenants/{tenant_id}/raw_data/{file_id}/drawing.dwg   ← file_key
    tenants/{tenant_id}/raw_data/{file_id}/drawing.pdf   ← pdf_key
    tenants/{tenant_id}/raw_data/{file_id}/drawing.png   ← thumbnail_key
"""

import os
import platform
import shutil
import subprocess
import threading
import time
from dataclasses import dataclass
from pathlib import Path

import fitz  # PyMuPDF
from loguru import logger

from app.core.config import settings
from app.infrastructure.s3_client import S3Client

# QCAD 동시 실행 제한 (CPU/메모리 집약적)
_semaphore = threading.Semaphore(settings.converter_max_concurrent)

_CAD_EXTS = {".dwg", ".dxf"}
_IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".tiff", ".tif"}


@dataclass
class ConversionResult:
    """변환 결과."""

    pdf_key: str
    pdf_content_type: str
    pdf_size: int
    thumbnail_key: str
    thumbnail_content_type: str
    thumbnail_size: int


def convert_drawing(file_key: str, s3: S3Client) -> ConversionResult:
    """통합 진입점: S3 다운로드 → PDF 변환 → 썸네일 생성 → S3 업로드.

    세마포어로 QCAD 동시 변환 수를 제한합니다.
    변환 결과는 S3 key 기반 로컬 캐시에 보관됩니다 (TTL 1시간).
    """
    with _semaphore:
        return _convert_pipeline(file_key, s3)


def get_cached_file(file_key: str) -> str | None:
    """S3 key에 대응하는 로컬 캐시 파일 경로 조회. 없으면 None."""
    path = _cache_path(file_key)
    return path if os.path.exists(path) else None


def cleanup_expired_cache(max_age_hours: int = 1) -> int:
    """만료된 변환 캐시 파일 삭제. 삭제 건수 반환."""
    logger.debug("변환 캐시 정리 시작 (TTL={hours}시간)", hours=max_age_hours)

    base = Path(settings.converter_temp_dir)
    if not base.exists():
        return 0

    now = time.time()
    max_age_sec = max_age_hours * 3600
    removed = 0

    # 만료된 파일 삭제
    for file_path in base.rglob("*"):
        if not file_path.is_file():
            continue
        if now - file_path.stat().st_mtime > max_age_sec:
            file_path.unlink(missing_ok=True)
            removed += 1

    # 빈 디렉토리 정리 (bottom-up)
    for dir_path in sorted(base.rglob("*"), reverse=True):
        if dir_path.is_dir() and not any(dir_path.iterdir()):
            dir_path.rmdir()

    if removed > 0:
        logger.info(
            "변환 캐시 정리: {removed}건 삭제 (TTL={hours}시간)",
            removed=removed,
            hours=max_age_hours,
        )
    return removed


# ── 내부 함수 ──


def _cache_path(key: str) -> str:
    """S3 key → 로컬 캐시 경로."""
    return os.path.join(settings.converter_temp_dir, key)


def _make_output_key(file_key: str, suffix: str) -> str:
    """원본 file_key의 확장자만 교체.

    예: tenants/.../drawing.dwg → tenants/.../drawing.pdf
    """
    return str(Path(file_key).with_suffix(suffix))


def _convert_pipeline(file_key: str, s3: S3Client) -> ConversionResult:
    """변환 파이프라인 내부 구현."""
    ext = Path(file_key).suffix.lower()
    input_path = _cache_path(file_key)
    pdf_key = _make_output_key(file_key, ".pdf")
    pdf_path = _cache_path(pdf_key)
    thumbnail_key = _make_output_key(file_key, ".png")
    thumbnail_path = _cache_path(thumbnail_key)

    os.makedirs(os.path.dirname(input_path), exist_ok=True)

    try:
        # 1. S3에서 원본 다운로드
        data = s3.get_object(file_key)
        with open(input_path, "wb") as f:
            f.write(data)

        # 2. PDF 확보 — 파일 타입에 따라 변환 경로 분기
        if ext in _CAD_EXTS:
            logger.info(
                "CAD→PDF 변환 선택: ext={ext} file_key={key}",
                ext=ext,
                key=file_key,
            )
            _generate_pdf_from_cad(input_path, pdf_path)
            pdf_size = _upload_file(s3, pdf_path, pdf_key, "application/pdf")
        elif ext in _IMAGE_EXTS:
            logger.info(
                "이미지→PDF 변환 선택: ext={ext} file_key={key}",
                ext=ext,
                key=file_key,
            )
            _generate_pdf_from_image(input_path, pdf_path)
            pdf_size = _upload_file(s3, pdf_path, pdf_key, "application/pdf")
        else:
            # PDF 원본 그대로 사용 — 변환 불필요
            logger.info(
                "PDF 원본 사용 (변환 스킵): ext={ext} file_key={key}",
                ext=ext,
                key=file_key,
            )
            pdf_key = file_key
            pdf_path = input_path
            meta = s3.head_object(file_key)
            pdf_size = meta["content_length"] if meta else 0

        # 3. 썸네일 생성 (항상 PDF에서)
        _generate_thumbnail(pdf_path, thumbnail_path)
        thumbnail_size = _upload_file(
            s3, thumbnail_path, thumbnail_key, "image/png"
        )

        return ConversionResult(
            pdf_key=pdf_key,
            pdf_content_type="application/pdf",
            pdf_size=pdf_size,
            thumbnail_key=thumbnail_key,
            thumbnail_content_type="image/png",
            thumbnail_size=thumbnail_size,
        )
    except Exception:
        # 변환 실패 시 캐시 즉시 삭제
        cache_dir = os.path.dirname(input_path)
        shutil.rmtree(cache_dir, ignore_errors=True)
        raise


def _generate_pdf_from_cad(input_path: str, output_path: str) -> None:
    """QCAD dwg2pdf CLI 호출."""
    cmd = [
        f"{settings.qcad_path}/dwg2pdf",
        "-f",  # 기존 파일 덮어쓰기
        "-auto-fit",
        "-auto-orientation",
        "-o",
        output_path,
        input_path,
    ]

    # macOS는 offscreen 플러그인 미지원 — Linux(Docker)에서만 사용
    if platform.system() != "Darwin":
        cmd.insert(1, "offscreen")
        cmd.insert(1, "-platform")

    result = subprocess.run(cmd, capture_output=True, timeout=300)

    if result.returncode != 0:
        stderr = result.stderr.decode(errors="replace")
        logger.error(
            "dwg2pdf 실패: exit_code={code} stderr={stderr} input={input}",
            code=result.returncode,
            stderr=stderr[:500],
            input=input_path,
        )
        raise RuntimeError(
            f"dwg2pdf 실패 (exit {result.returncode}): {stderr[:500]}"
        )


def _generate_pdf_from_image(input_path: str, output_path: str) -> None:
    """이미지(JPG/PNG/BMP/TIFF) → PDF 변환."""
    img_doc = fitz.open(input_path)
    pdf_bytes = img_doc.convert_to_pdf()
    img_doc.close()

    pdf_doc = fitz.open("pdf", pdf_bytes)
    pdf_doc.save(output_path)
    pdf_doc.close()


def _generate_thumbnail(pdf_path: str, output_path: str, dpi: int = 150) -> None:
    """PDF 첫 페이지 → PNG 썸네일."""
    doc = fitz.open(pdf_path)
    zoom = dpi / 72
    matrix = fitz.Matrix(zoom, zoom)

    pix = doc[0].get_pixmap(matrix=matrix)
    pix.save(output_path)
    doc.close()


def _upload_file(
    s3: S3Client, local_path: str, file_key: str, content_type: str
) -> int:
    """로컬 파일을 S3에 업로드. 파일 크기 반환."""
    with open(local_path, "rb") as f:
        data = f.read()
    return s3.put_object(file_key, data, content_type)
