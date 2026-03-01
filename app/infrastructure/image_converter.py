"""도면 이미지 변환 유틸리티.

PDF → 페이지별 WebP 변환, 텍스트 추출, 이미지 리사이징 등을 제공합니다.
PyMuPDF(fitz)로 PDF 처리, Pillow로 이미지 변환/리사이징.
"""

import io

import fitz  # PyMuPDF
from loguru import logger
from PIL import Image


def detect_drawing_type(filename: str) -> str:
    """파일명으로 도면 타입 판별.

    Returns:
        "pdf" | "image" | "unsupported"
    """
    lower = filename.lower()
    if lower.endswith(".pdf"):
        return "pdf"
    if lower.endswith((".png", ".jpg", ".jpeg")):
        return "image"
    return "unsupported"


def pdf_extract_text(content: bytes, max_pages: int = 20) -> dict:
    """PDF에서 텍스트 + 테이블 직접 추출 (Vision LLM 보조 데이터용).

    Returns:
        {
            "pages": [
                {
                    "page_num": 1,
                    "text_blocks": [
                        {"text": "DWG-001", "bbox": [x0, y0, x1, y1], "font_size": 12.0}
                    ],
                    "tables": [
                        [["품번", "품명", "수량"], ["BOLT-M10", "볼트", "4"]]
                    ]
                }
            ],
            "has_meaningful_text": True,
            "page_count": 3,
        }
    """
    doc = fitz.open(stream=content, filetype="pdf")
    page_count = min(len(doc), max_pages)
    pages: list[dict] = []
    total_text_len = 0

    for i in range(page_count):
        page = doc[i]
        page_data: dict = {"page_num": i + 1, "text_blocks": [], "tables": []}

        # 텍스트 블록 추출 (위치 + 폰트 크기 포함)
        text_dict = page.get_text("dict")
        for block in text_dict.get("blocks", []):
            if block.get("type") != 0:  # 텍스트 블록만
                continue
            for line in block.get("lines", []):
                for span in line.get("spans", []):
                    text = span.get("text", "").strip()
                    if not text:
                        continue
                    page_data["text_blocks"].append({
                        "text": text,
                        "bbox": list(span.get("bbox", [])),
                        "font_size": round(span.get("size", 0), 1),
                    })
                    total_text_len += len(text)

        # 테이블 추출
        try:
            tables = page.find_tables()
            for table in tables:
                rows = table.extract()
                if rows:
                    # None 값을 빈 문자열로 치환
                    cleaned = [
                        [cell if cell is not None else "" for cell in row]
                        for row in rows
                    ]
                    page_data["tables"].append(cleaned)
        except Exception:
            pass  # 테이블 감지 실패 시 무시

        pages.append(page_data)

    doc.close()

    # 벡터 PDF vs 스캔 PDF 판별: 첫 페이지 텍스트 > 50자
    first_page_text_len = sum(
        len(b["text"]) for b in pages[0]["text_blocks"]
    ) if pages else 0
    has_meaningful_text = first_page_text_len > 50

    logger.info(
        "PDF 텍스트 추출: {pages}페이지, 총 {chars}자, meaningful={meaningful}",
        pages=page_count,
        chars=total_text_len,
        meaningful=has_meaningful_text,
    )

    return {
        "pages": pages,
        "has_meaningful_text": has_meaningful_text,
        "page_count": page_count,
    }


def pdf_to_images(
    content: bytes,
    dpi: int = 200,
    max_pages: int = 20,
) -> list[bytes]:
    """PDF 바이트 → 페이지별 WebP 바이트 리스트 변환.

    Args:
        content: PDF 파일 바이트
        dpi: 렌더링 해상도 (기본 200)
        max_pages: 최대 처리 페이지 수

    Returns:
        페이지별 WebP 바이트 리스트
    """
    doc = fitz.open(stream=content, filetype="pdf")
    page_count = min(len(doc), max_pages)
    images: list[bytes] = []

    zoom = dpi / 72
    matrix = fitz.Matrix(zoom, zoom)

    for i in range(page_count):
        page = doc[i]
        pix = page.get_pixmap(matrix=matrix)
        png_bytes = pix.tobytes("png")

        # PNG → WebP 무손실 변환
        img = Image.open(io.BytesIO(png_bytes))
        buf = io.BytesIO()
        img.save(buf, format="WEBP", lossless=True)
        images.append(buf.getvalue())

    doc.close()
    logger.info("PDF→WebP 변환: {pages}페이지 (dpi={dpi})", pages=page_count, dpi=dpi)
    return images


def ensure_webp(
    content: bytes,
    max_dim: int = 2048,
) -> bytes:
    """PNG/JPG 이미지 → 리사이징된 무손실 WebP 바이트 변환.

    장축이 max_dim을 초과하면 비율 유지하며 축소합니다.
    """
    img = Image.open(io.BytesIO(content))

    # RGBA → RGB 변환 (JPEG 소스일 수 있음)
    if img.mode == "RGBA":
        img = img.convert("RGB")

    # 장축 기준 리사이징
    w, h = img.size
    if max(w, h) > max_dim:
        scale = max_dim / max(w, h)
        new_w = int(w * scale)
        new_h = int(h * scale)
        img = img.resize((new_w, new_h), Image.LANCZOS)
        logger.info("이미지 리사이징: {w}x{h} → {nw}x{nh}", w=w, h=h, nw=new_w, nh=new_h)

    buf = io.BytesIO()
    img.save(buf, format="WEBP", lossless=True)
    return buf.getvalue()


def create_thumbnail(content: bytes, size: int = 256) -> bytes:
    """이미지를 center crop 후 정사각 WebP 썸네일로 변환.

    짧은 변 기준으로 중앙 크롭 → size×size 리사이징 → WebP 변환.
    """
    img = Image.open(io.BytesIO(content))

    if img.mode == "RGBA":
        img = img.convert("RGB")

    # center crop: 짧은 변 기준 정사각 크롭
    w, h = img.size
    crop_size = min(w, h)
    left = (w - crop_size) // 2
    top = (h - crop_size) // 2
    img = img.crop((left, top, left + crop_size, top + crop_size))

    # 리사이징
    img = img.resize((size, size), Image.LANCZOS)

    buf = io.BytesIO()
    img.save(buf, format="WEBP", quality=85)
    return buf.getvalue()
