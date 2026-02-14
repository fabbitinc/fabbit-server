"""합성(Synthesis) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class SynthesisStartRequest(BaseModel):
    """합성 작업 시작 요청."""

    upload_id: uuid.UUID
    mapping_id: uuid.UUID | None = None


class SynthesisBatchStartRequest(BaseModel):
    """합성 배치 작업 시작 요청."""

    upload_ids: list[uuid.UUID] = Field(
        ...,
        min_length=1,
        max_length=100,
        description="합성할 업로드 ID 목록",
    )
    mapping_id: uuid.UUID | None = Field(
        None,
        description="사용할 매핑 ID (미지정 시 프로젝트/조직 최신 매핑 자동 선택)",
    )


class SynthesisBatchFailure(BaseModel):
    """배치 시작 실패 항목."""

    upload_id: uuid.UUID
    reason: str


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


class SynthesisBatchStartResponse(BaseModel):
    """합성 배치 작업 시작 응답."""

    batch_id: uuid.UUID
    requested_count: int
    accepted_count: int
    items: list[SynthesisJobResponse]
    failed: list[SynthesisBatchFailure]


class SynthesisBatchItemStatus(BaseModel):
    """배치 내 개별 작업 상태."""

    job_id: uuid.UUID
    upload_id: uuid.UUID
    status: str
    total_rows: int
    processed_rows: int
    nodes_created: int
    relationships_created: int
    error_count: int
    started_at: datetime | None
    completed_at: datetime | None


class SynthesisBatchStatusResponse(BaseModel):
    """합성 배치 작업 진행 상태 응답."""

    batch_id: uuid.UUID
    requested_count: int
    accepted_count: int
    failed_count: int
    pending_count: int
    processing_count: int
    completed_count: int
    failed_job_count: int
    status: str
    failed: list[SynthesisBatchFailure]
    items: list[SynthesisBatchItemStatus]
    created_at: datetime
