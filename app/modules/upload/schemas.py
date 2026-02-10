"""업로드 API 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class CreateUploadRequest(BaseModel):
    original_name: str = Field(..., max_length=500, description="원본 파일명")
    content_type: str = Field(..., max_length=100, description="MIME 타입")
    file_size: int = Field(..., gt=0, description="파일 크기 (바이트)")
    project_id: uuid.UUID | None = Field(None, description="연결할 프로젝트 ID")


class CreateUploadResponse(BaseModel):
    upload_id: uuid.UUID
    upload_url: str
    file_key: str


class UploadCompleteResponse(BaseModel):
    upload_id: uuid.UUID
    status: str
    original_name: str
    file_key: str
    file_size: int
    content_type: str
    created_at: datetime
