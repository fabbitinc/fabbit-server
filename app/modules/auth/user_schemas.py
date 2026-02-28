"""사용자 프로필 Pydantic 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


# ── 프로필 수정 ──


class UpdateProfileRequest(BaseModel):
    full_name: str | None = Field(None, min_length=1, max_length=100)


class UpdateProfileResponse(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str
    updated_at: datetime

    model_config = {"from_attributes": True}


# ── 비밀번호 변경 ──


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str = Field(min_length=8, max_length=128)
