"""조직 Pydantic 요청/응답 스키마."""

import uuid

from pydantic import BaseModel, Field

from app.modules.user.schemas import UserResponse


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


class MeResponse(BaseModel):
    user: UserResponse
    memberships: list[MembershipResponse]


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


class SwitchOrgRequest(BaseModel):
    slug: str = Field(min_length=1, max_length=50, description="전환할 워크스페이스 주소")
