"""매핑 API Pydantic 스키마."""

import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

from app.modules.ontology.schemas import MappingResult


class MappingPreviewRequest(BaseModel):
    """매핑 미리보기 요청"""

    upload_id: uuid.UUID
    sheet_name: str | None = None  # Excel 시트명 (None이면 모든 시트)


class SheetPreview(BaseModel):
    """개별 시트 미리보기 결과"""

    sheet_name: str
    headers: list[str]
    sample_rows: list[dict]
    mapping: MappingResult


class SkippedSheet(BaseModel):
    """스킵된 시트 정보"""

    sheet_name: str
    reason: str


class MappingPreviewResponse(BaseModel):
    """매핑 미리보기 응답 — LLM 분석 결과"""

    headers: list[str]
    sample_rows: list[dict]
    mapping: MappingResult
    sheets: list[SheetPreview] = []
    skipped_sheets: list[SkippedSheet] = []


class MappingConfirmRequest(BaseModel):
    """매핑 확정 요청 — 사용자 검토 후 확인"""

    upload_id: uuid.UUID
    name: str
    sheet_name: str | None = None  # Excel 시트명 (None이면 모든 시트)
    mapping: MappingResult


class MappingUpdateRequest(BaseModel):
    """매핑 업데이트 요청 — 새 리비전 생성"""

    upload_id: uuid.UUID
    name: str | None = None
    sheet_name: str | None = None
    mapping: MappingResult


class MappingResponse(BaseModel):
    """매핑 레코드 응답"""

    id: uuid.UUID
    upload_id: uuid.UUID
    name: str
    sheet_name: str | None = None
    original_headers: list[str]
    mapped_headers: list[str]
    mapping: MappingResult
    scope: str
    is_active: bool = True
    usage_count: int
    version: int
    created_at: datetime

    model_config = {"from_attributes": True}


class MappingListResponse(BaseModel):
    """매핑 목록 응답"""

    items: list[MappingResponse]


class ValidationIssue(BaseModel):
    """매핑 검증 이슈"""

    code: str
    severity: Literal["error", "warning"]
    message: str
    path: str = ""
    dismissed_reason: str | None = None


class MappingImpactSummary(BaseModel):
    """매핑 변경 영향 요약"""

    disabled_column_count: int = 0


class MappingValidateRequest(BaseModel):
    """매핑 검증 요청"""

    upload_id: uuid.UUID
    sheet_name: str | None = None
    mapping: MappingResult


class MappingValidateResponse(BaseModel):
    """매핑 검증 응답"""

    normalized_mapping: MappingResult
    errors: list[ValidationIssue] = Field(default_factory=list)
    warnings: list[ValidationIssue] = Field(default_factory=list)
    impact_summary: MappingImpactSummary = Field(default_factory=MappingImpactSummary)


