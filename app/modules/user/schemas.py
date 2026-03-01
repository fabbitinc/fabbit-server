"""사용자 Pydantic 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class UserSummary(BaseModel):
    """유저 요약 정보 (타임라인 등 임베딩용)."""

    id: uuid.UUID
    full_name: str
    profile_image_url: str | None = None


class UserResponse(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str
    phone: str | None = None
    profile_image_url: str | None = None
    is_active: bool
    created_at: datetime

    model_config = {"from_attributes": True}


# ── 프로필 수정 ──


class UpdateProfileRequest(BaseModel):
    full_name: str | None = Field(None, min_length=1, max_length=100)
    phone: str | None = Field(None, max_length=20)


class UpdateProfileResponse(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str
    phone: str | None = None
    profile_image_url: str | None = None
    updated_at: datetime

    model_config = {"from_attributes": True}


# ── 비밀번호 변경 ──


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str = Field(min_length=8, max_length=128)


# ── 프로필 이미지 ──


class SetProfileImageRequest(BaseModel):
    file_id: uuid.UUID


class ProfileImageResponse(BaseModel):
    profile_image_url: str
