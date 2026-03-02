"""인증/초대 Pydantic 요청/응답 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel, EmailStr, Field

from app.modules.organization.schemas import OrganizationResponse
from app.modules.user.schemas import UserResponse


# ── 이메일 인증 ──


class SendVerificationRequest(BaseModel):
    email: EmailStr
    turnstile_token: str | None = None


class SendVerificationResponse(BaseModel):
    message: str


class VerifyEmailRequest(BaseModel):
    email: EmailStr
    code: str = Field(min_length=6, max_length=6)


class VerifyEmailResponse(BaseModel):
    verification_token: str
    email: str


class CheckEmailResponse(BaseModel):
    available: bool
    message: str | None = None


# ── 토큰 ──


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshRequest(BaseModel):
    refresh_token: str


# ── 회원가입 ──


class RegisterRequest(BaseModel):
    verification_token: str = Field(description="이메일 인증 완료 증표")
    code: str = Field(min_length=6, max_length=6, description="이메일 인증코드 (재검증용)")
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=100)
    org_name: str = Field(min_length=1, max_length=100)
    slug: str | None = Field(None, min_length=3, max_length=50, description="커스텀 워크스페이스 주소")
    industry: str | None = Field(None, max_length=50, description="산업 분야")
    team_size: str | None = Field(None, max_length=20, description="팀 규모")
    job_role: str | None = Field(None, max_length=50, description="직무")
    plan_type: str = Field("FREE", description="요금제 (FREE / PRO / ELITE)")
    turnstile_token: str | None = Field(None, description="Cloudflare Turnstile 토큰")


class RegisterResponse(BaseModel):
    user: UserResponse
    organization: OrganizationResponse
    tokens: TokenResponse


# ── 로그인 ──


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class LoginResponse(BaseModel):
    user: UserResponse
    tokens: TokenResponse


class ScopedLoginResponse(BaseModel):
    """slug 없이 로그인 시 반환 — 조직 생성 전용 스코프 토큰."""
    user: UserResponse
    scoped_token: str


# ── 조직 생성/전환 응답 (토큰 포함이므로 auth에 유지) ──


class CreateOrganizationResponse(BaseModel):
    organization: OrganizationResponse
    tokens: TokenResponse


# ── 초대 ──


class CreateInvitationRequest(BaseModel):
    email: EmailStr
    role: str = Field("MEMBER", description="초대할 역할 (MEMBER / ADMIN / OWNER)")


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
