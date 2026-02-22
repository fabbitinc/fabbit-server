"""합성(Synthesis) API Pydantic 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class SynthesisUploadItem(BaseModel):
    """합성 대상 파일 항목."""

    file_id: uuid.UUID
    root_context: dict[str, str] | None = None  # ROOT_BOM: {"Part": "ASM-001", "Supplier": "ACME"}


class SynthesisStartRequest(BaseModel):
    """통합 합성 시작 요청."""

    mapping_id: uuid.UUID
    project_id: uuid.UUID | None = None  # 프로젝트 소속 검증 (선택)
    overwrite: bool = False  # 기존 데이터 덮어쓰기 (엑셀에 값이 있는 필드만)
    uploads: list[SynthesisUploadItem] = Field(..., min_length=1, max_length=100)


class SynthesisBatchFailure(BaseModel):
    """배치 시작 실패 항목."""

    file_id: uuid.UUID
    reason: str


class SynthesisJobResponse(BaseModel):
    """합성 작업 상태 응답."""

    id: uuid.UUID
    mapping_id: uuid.UUID
    file_id: uuid.UUID
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
    file_id: uuid.UUID
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
