"""사용자 프로필 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.organization.schemas import MeResponse
from app.modules.user.schemas import (
    ChangePasswordRequest,
    ProfileImageResponse,
    SetProfileImageRequest,
    UpdateProfileRequest,
    UpdateProfileResponse,
)
from app.queries import user as user_queries
from app.use_cases import user as user_commands

router = APIRouter(prefix="/api/v1/users", tags=["users"])


@router.get("/me", response_model=MeResponse)
def me(
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """내 정보 조회.

    현재 인증된 사용자의 기본 정보와 소속 조직 목록을 반환합니다.
    """
    return user_queries.get_me(db, auth)


@router.patch("/me", response_model=UpdateProfileResponse)
def update_profile(
    req: UpdateProfileRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """내 프로필 수정.

    전달된 필드만 업데이트합니다 (partial update).
    - **full_name**: 이름 변경 (1~100자)
    """
    return user_commands.update_profile(db, auth, req)


@router.put("/me/password", status_code=204)
def change_password(
    req: ChangePasswordRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_auth),
):
    """비밀번호 변경.

    현재 비밀번호를 검증한 후 새 비밀번호로 변경합니다.
    - **current_password**: 현재 비밀번호
    - **new_password**: 새 비밀번호 (8~128자)
    """
    user_commands.change_password(db, auth, req)


@router.put("/me/profile-image", response_model=ProfileImageResponse)
def set_profile_image(
    req: SetProfileImageRequest,
    db: Session = Depends(get_tenant_db),
    auth: AuthContext = Depends(require_auth),
):
    """프로필 이미지 설정.

    기존 파일 업로드 API로 업로드한 파일의 file_id를 전달하여 프로필 이미지로 설정합니다.
    파일 상태(UPLOADED) 및 미연결 여부를 검증한 후 저장합니다.
    """
    return user_commands.set_profile_image(db, auth, req.file_id)


@router.delete("/me/profile-image", status_code=204)
def delete_profile_image(
    db: Session = Depends(get_tenant_db),
    auth: AuthContext = Depends(require_auth),
):
    """프로필 이미지 제거.

    프로필 이미지를 제거하고 연결된 파일을 소프트 삭제합니다.
    """
    user_commands.delete_profile_image(db, auth)
