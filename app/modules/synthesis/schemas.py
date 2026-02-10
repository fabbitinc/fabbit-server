"""합성(Synthesis) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel


class SynthesisStartRequest(BaseModel):
    """합성 작업 시작 요청."""
    mapping_id: uuid.UUID


class SynthesisJobResponse(BaseModel):
    """합성 작업 상태 응답."""
    id: uuid.UUID
    mapping_id: uuid.UUID
    upload_id: uuid.UUID
    status: str
    total_rows: int
    processed_rows: int
    nodes_created: int
    relationships_created: int
    errors: list[str]
    started_at: datetime | None
    completed_at: datetime | None
    created_at: datetime


class SynthesisListResponse(BaseModel):
    """합성 작업 목록 응답."""
    items: list[SynthesisJobResponse]
