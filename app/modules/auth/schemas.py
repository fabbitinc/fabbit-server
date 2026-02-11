"""인증 Pydantic 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


# ── 요청 ──


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=100)
    org_name: str = Field(min_length=1, max_length=100)
    slug: str | None = Field(None, min_length=3, max_length=50, description="커스텀 워크스페이스 주소")
    industry: str | None = Field(None, max_length=50, description="산업 분야")
    team_size: str | None = Field(None, max_length=20, description="팀 규모")
    job_role: str | None = Field(None, max_length=50, description="직무")
    plan_type: str = Field("FREE", description="요금제 (FREE / PRO / ELITE)")
    turnstile_token: str | None = Field(None, description="Cloudflare Turnstile 토큰")


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
    industry: str | None = None
    team_size: str | None = None
    plan_type: str

    model_config = {"from_attributes": True}


class MembershipResponse(BaseModel):
    org_id: uuid.UUID
    role: str
    job_role: str | None = None
    organization: OrganizationResponse

    model_config = {"from_attributes": True}


class RegisterResponse(BaseModel):
    user: UserResponse
    organization: OrganizationResponse
    tokens: TokenResponse


class LoginResponse(BaseModel):
    user: UserResponse
    tokens: TokenResponse


class MeResponse(BaseModel):
    user: UserResponse
    memberships: list[MembershipResponse]


# ── 사전 검증 ──


class CheckEmailResponse(BaseModel):
    available: bool
    message: str | None = None


class CheckSlugResponse(BaseModel):
    available: bool
    message: str | None = None
    suggestion: str | None = None


class PlanResponse(BaseModel):
    plan_type: str
    display_name: str
    description: str
    max_members: int
    storage_gb: int
    max_bom: int
    max_drawing_parses: int
    price_monthly: int
