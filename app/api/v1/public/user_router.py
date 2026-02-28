"""사용자 프로필 API 라우터."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.auth.user_schemas import (
    ChangePasswordRequest,
    UpdateProfileRequest,
    UpdateProfileResponse,
)
from app.use_cases import user as user_commands

router = APIRouter(prefix="/api/v1/users", tags=["users"])


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
