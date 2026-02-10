"""인증 Pydantic 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


# ── 요청 ──


class SignupRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=100)
    org_name: str = Field(min_length=1, max_length=100)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RefreshRequest(BaseModel):
    refresh_token: str


# ── 응답 ──


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str
    is_active: bool
    created_at: datetime

    model_config = {"from_attributes": True}


class OrganizationResponse(BaseModel):
    id: uuid.UUID
    slug: str
    name: str

    model_config = {"from_attributes": True}


class MembershipResponse(BaseModel):
    org_id: uuid.UUID
    role: str
    organization: OrganizationResponse

    model_config = {"from_attributes": True}


class SignupResponse(BaseModel):
    user: UserResponse
    organization: OrganizationResponse
    tokens: TokenResponse


class LoginResponse(BaseModel):
    user: UserResponse
    tokens: TokenResponse


class MeResponse(BaseModel):
    user: UserResponse
    memberships: list[MembershipResponse]
