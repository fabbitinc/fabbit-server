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
    onboarded_at: datetime | None = None

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


class ScopedLoginResponse(BaseModel):
    """slug 없이 로그인 시 반환 — 조직 생성 전용 스코프 토큰."""
    user: UserResponse
    scoped_token: str


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


class SiteResponse(BaseModel):
    """서브도메인 접근 시 워크스페이스 기본 정보 (로그인 페이지용)."""
    slug: str
    name: str

    model_config = {"from_attributes": True}


class PlanResponse(BaseModel):
    plan_type: str
    display_name: str
    description: str
    storage_gb: int
    max_bom: int
    max_drawing_parses: int
    max_chats: int
    price_monthly: int


# ── 조직 생성 / 전환 ──


class CreateOrganizationRequest(BaseModel):
    org_name: str = Field(min_length=1, max_length=100)
    slug: str | None = Field(None, min_length=3, max_length=50, description="커스텀 워크스페이스 주소")
    industry: str | None = Field(None, max_length=50, description="산업 분야")
    team_size: str | None = Field(None, max_length=20, description="팀 규모")
    plan_type: str = Field("FREE", description="요금제 (FREE / PRO / ELITE)")


class CreateOrganizationResponse(BaseModel):
    organization: OrganizationResponse
    tokens: TokenResponse


class SwitchOrgRequest(BaseModel):
    slug: str = Field(min_length=1, max_length=50, description="전환할 워크스페이스 주소")


# ── 초대 ──


class CreateInvitationRequest(BaseModel):
    email: EmailStr
    role: str = Field("MEMBER", description="초대할 역할 (MEMBER / ADMIN)")


class VerifyInvitationResponse(BaseModel):
    email: str
    org_name: str
    inviter_name: str
    role: str
    is_existing_user: bool
    expires_at: datetime


class AcceptInvitationRequest(BaseModel):
    token: str
    password: str | None = Field(None, min_length=8, max_length=128, description="미가입자인 경우 필수")
    full_name: str | None = Field(None, min_length=1, max_length=100, description="미가입자인 경우 필수")


class InvitationResponse(BaseModel):
    id: uuid.UUID
    org_id: uuid.UUID
    email: str
    role: str
    status: str
    invited_by: uuid.UUID
    expires_at: datetime
    accepted_at: datetime | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class InvitationListResponse(BaseModel):
    invitations: list[InvitationResponse]


class AcceptInvitationResponse(BaseModel):
    user: UserResponse
    organization: OrganizationResponse
    tokens: TokenResponse
    is_new_user: bool
