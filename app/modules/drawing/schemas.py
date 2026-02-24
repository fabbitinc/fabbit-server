"""도면 분석 API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.modules.drawing.constants import ConversionStatus

# ── LLM 추출 결과 ──


class ExtractedTitleBlock(BaseModel):
    """표제란 추출 결과."""

    drawing_number: str | None = None
    name: str | None = None
    version: str | None = None
    date: str | None = None
    author: str | None = None
    sheet_info: str | None = None
    additional: dict[str, str] = {}


class ExtractedPart(BaseModel):
    """부품 목록 추출 결과."""

    reference_designator: str | None = None
    part_number: str | None = None
    name: str | None = None
    quantity: int = 1
    value: str | None = None
    package: str | None = None


class DrawingAnalysisResult(BaseModel):
    """도면 분석 전체 결과."""

    title_block: ExtractedTitleBlock
    parts: list[ExtractedPart]
    drawing_type: str = "unknown"
    confidence: int = 0
    notes: str = ""


# ── 매칭 리포트 ──


class PartMatch(BaseModel):
    """기존 Part와 일치한 부품."""

    extracted: ExtractedPart
    existing_part_number: str
    existing_name: str | None = None


class PartConflict(BaseModel):
    """속성 불일치 부품."""

    part_number: str
    field: str
    extracted_value: str
    existing_value: str


class MatchingReport(BaseModel):
    """기존 BOM 데이터와의 매칭 결과."""

    matched_parts: list[PartMatch] = []
    new_parts: list[ExtractedPart] = []
    conflicting_parts: list[PartConflict] = []


# ── API 요청/응답 ──


class DrawingAnalyzeRequest(BaseModel):
    """도면 분석 요청."""

    file_id: uuid.UUID
    page_range: str | None = None


class DrawingAnalyzeResponse(BaseModel):
    """도면 분석 미리보기 응답."""

    file_id: uuid.UUID
    page_count: int
    analysis: DrawingAnalysisResult
    matching_report: MatchingReport | None = None
    extraction_method: str = "vision_llm"


class DrawingConfirmRequest(BaseModel):
    """분석 결과 확정 요청."""

    file_id: uuid.UUID
    name: str
    analysis: DrawingAnalysisResult


class DrawingAnalysisResponse(BaseModel):
    """분석 레코드 응답 (DB 저장 후)."""

    id: uuid.UUID
    file_id: uuid.UUID
    name: str
    analysis: dict
    page_count: int
    created_at: datetime


class DrawingAnalysisListResponse(BaseModel):
    """분석 레코드 목록 응답."""

    items: list[DrawingAnalysisResponse]


class DrawingSynthesisStartRequest(BaseModel):
    """도면 합성 시작 요청."""

    analysis_id: uuid.UUID


class DrawingSynthesisJobResponse(BaseModel):
    """도면 합성 작업 상태 응답."""

    id: uuid.UUID
    analysis_id: uuid.UUID
    status: str
    nodes_created: int
    relationships_created: int
    errors: list[str]
    started_at: datetime | None = None
    completed_at: datetime | None = None
    created_at: datetime


# ── Drawing 검색 (온톨로지 dual-write) ──


# ── Drawing 등록 ──


class RegisterDrawingRequest(BaseModel):
    """단건 도면 등록 요청."""

    file_id: uuid.UUID


class RegisterDrawingResponse(BaseModel):
    """도면 등록 응답."""

    drawing_id: uuid.UUID
    drawing_number: str | None = None
    name: str
    conversion_status: ConversionStatus | None = None


class BulkRegisterDrawingItem(BaseModel):
    """대량 도면 등록 개별 항목."""

    file_id: uuid.UUID
    part_id: uuid.UUID | None = None


class BulkRegisterDrawingRequest(BaseModel):
    """대량 도면 등록 요청."""

    items: list[BulkRegisterDrawingItem] = Field(..., min_length=1, max_length=100)


class BulkRegisterDrawingFailure(BaseModel):
    """대량 도면 등록 실패 항목."""

    file_id: uuid.UUID
    reason: str


class BulkRegisterDrawingResponse(BaseModel):
    """대량 도면 등록 응답."""

    items: list[RegisterDrawingResponse]
    failed: list[BulkRegisterDrawingFailure]


# ── Drawing 검색 (온톨로지 dual-write) ──


class DrawingSummary(BaseModel):
    id: uuid.UUID
    drawing_number: str | None = None
    name: str
    version: str | None = None
    status: str | None = None
    original_file_key: str | None = None
    pdf_key: str | None = None
    thumbnail_key: str | None = None
    conversion_status: ConversionStatus | None = None


class DrawingListResponse(BaseModel):
    total: int
    offset: int
    limit: int
    items: list[DrawingSummary]
