"""파일 API 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class CreateFileRequest(BaseModel):
    original_name: str = Field(..., max_length=500, description="원본 파일명")
    content_type: str = Field(..., max_length=100, description="MIME 타입")
    file_size: int = Field(..., gt=0, description="파일 크기 (바이트)")
    owner_type: str | None = Field(None, description="소유자 타입 (project, folder, part, ...)")
    owner_id: uuid.UUID | None = Field(None, description="소유자 엔티티 ID")


class CreateFileResponse(BaseModel):
    file_id: uuid.UUID
    upload_url: str
    file_key: str


class BatchCreateFileRequest(BaseModel):
    items: list[CreateFileRequest] = Field(
        ..., min_length=1, max_length=100, description="업로드할 파일 목록 (최대 100개)"
    )


class BatchCreateFileResponse(BaseModel):
    items: list[CreateFileResponse]


class BatchCompleteRequest(BaseModel):
    file_ids: list[uuid.UUID] = Field(
        ..., min_length=1, max_length=100, description="완료 확인할 파일 ID 목록"
    )


class BatchCompleteResponse(BaseModel):
    items: list["FileCompleteResponse"]
    failed: list["BatchCompleteFailure"]


class BatchCompleteFailure(BaseModel):
    file_id: uuid.UUID
    reason: str


class FileCompleteResponse(BaseModel):
    file_id: uuid.UUID
    status: str
    original_name: str
    file_key: str
    file_size: int
    content_type: str
    created_at: datetime
