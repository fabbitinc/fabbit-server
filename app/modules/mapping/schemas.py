"""매핑 API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel

from app.modules.ontology.schemas import MappingResult


class MappingPreviewRequest(BaseModel):
    """매핑 미리보기 요청"""
    upload_id: uuid.UUID
    header_row: int = 1  # 헤더 행 번호 (1부터 시작)


class MappingPreviewResponse(BaseModel):
    """매핑 미리보기 응답 — LLM 분석 결과"""
    headers: list[str]
    sample_rows: list[dict]
    mapping: MappingResult


class MappingConfirmRequest(BaseModel):
    """매핑 확정 요청 — 사용자 검토 후 확인"""
    upload_id: uuid.UUID
    name: str
    header_row: int = 1  # 헤더 행 번호 (1부터 시작)
    mapping: MappingResult


class MappingResponse(BaseModel):
    """매핑 레코드 응답"""
    id: uuid.UUID
    upload_id: uuid.UUID
    name: str
    original_headers: list[str]
    mapping: MappingResult
    usage_count: int
    created_at: datetime

    model_config = {"from_attributes": True}


class MappingListResponse(BaseModel):
    """매핑 목록 응답"""
    items: list[MappingResponse]
