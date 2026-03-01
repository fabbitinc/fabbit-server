"""조직 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_tenant_db, require_admin, require_auth, require_create_org_token
from app.core.auth_context import AuthContext, CreateOrgContext
from app.modules.auth.schemas import CreateOrganizationResponse, LoginResponse
from app.modules.organization.schemas import (
    CreateOrganizationRequest,
    ProfileImageResponse,
    SetProfileImageRequest,
    SwitchOrgRequest,
)
from app.use_cases import organization as org_commands

router = APIRouter(prefix="/api/v1/organizations", tags=["organizations"])


@router.post("", response_model=CreateOrganizationResponse)
def create_organization(
    req: CreateOrganizationRequest,
    db: Session = Depends(get_db),
    ctx: CreateOrgContext = Depends(require_create_org_token),
):
    """기가입자 조직 생성.

    루트 도메인 로그인으로 발급받은 **스코프 토큰**(scope=create_org)이 필요합니다.
    조직 생성 + ADMIN 멤버십 + 테넌트 프로비저닝 후 정상 access+refresh 토큰을 반환합니다.
    """
    return org_commands.create_organization(db, ctx.user_id, req)


@router.post("/switch", response_model=LoginResponse)
def switch_org(
    req: SwitchOrgRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """조직 전환.

    현재 인증된 유저가 다른 워크스페이스로 전환합니다.
    대상 워크스페이스의 멤버십을 확인한 후 새 access+refresh 토큰을 발급합니다.
    """
    return org_commands.switch_org(db, auth.user_id, auth.email, req.slug)


@router.put("/profile-image", response_model=ProfileImageResponse)
def set_profile_image(
    req: SetProfileImageRequest,
    db: Session = Depends(get_tenant_db),
    auth: AuthContext = Depends(require_admin),
):
    """조직 프로필 이미지 설정.

    기존 파일 업로드 API로 업로드한 파일의 file_id를 전달하여 조직 프로필 이미지로 설정합니다.
    파일 상태(UPLOADED) 및 미연결 여부를 검증한 후 저장합니다.
    **ADMIN 권한 필요.**
    """
    return org_commands.set_profile_image(db, auth, req.file_id)


@router.delete("/profile-image", status_code=204)
def delete_profile_image(
    db: Session = Depends(get_tenant_db),
    auth: AuthContext = Depends(require_admin),
):
    """조직 프로필 이미지 제거.

    프로필 이미지를 제거하고 연결된 파일을 소프트 삭제합니다.
    **ADMIN 권한 필요.**
    """
    org_commands.delete_profile_image(db, auth)
