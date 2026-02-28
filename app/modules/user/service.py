"""사용자 비즈니스 로직."""

from __future__ import annotations

from typing import TYPE_CHECKING

from sqlalchemy.orm import Session

if TYPE_CHECKING:
    from app.core.auth_context import AuthContext
    from app.modules.user.schemas import (
        ChangePasswordRequest,
        UpdateProfileRequest,
        UpdateProfileResponse,
    )

from app.core.exceptions import AppError
from app.infrastructure.password_hasher import hash_password, verify_password
from app.modules.user import repository as repo
from app.modules.user.models import User


def create_user(db: Session, email: str, password: str, full_name: str) -> User:
    """유저 생성 (비밀번호 해싱 포함).

    register/accept_invitation use_case에서 호출.
    """
    hashed = hash_password(password)
    return repo.create_user(db, email, hashed, full_name)


def authenticate(db: Session, email: str, password: str) -> User:
    """자격증명 검증.

    login use_case에서 호출.
    이메일/비밀번호 불일치 또는 비활성 계정 시 AppError.
    """
    user = repo.get_user_by_email(db, email)
    if not user or not verify_password(password, user.hashed_password):
        raise AppError(
            message="이메일 또는 비밀번호가 올바르지 않습니다",
            code="INVALID_CREDENTIALS",
        )
    if not user.is_active:
        raise AppError(message="비활성화된 계정입니다", code="FORBIDDEN")
    return user


def get_user_or_raise(db: Session, user_id) -> User:
    """유저 조회 + 404 처리."""
    user = repo.get_user_by_id(db, user_id)
    if not user:
        raise AppError(message="사용자를 찾을 수 없습니다", code="NOT_FOUND")
    return user


def update_profile(
    db: Session, auth: AuthContext, req: UpdateProfileRequest
) -> UpdateProfileResponse:
    """프로필 수정 (partial update).

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    from app.modules.user.schemas import UpdateProfileResponse

    user = get_user_or_raise(db, auth.user_id)

    updates = req.model_dump(exclude_unset=True)
    if not updates:
        return UpdateProfileResponse.model_validate(user)

    for field, value in updates.items():
        setattr(user, field, value)
    db.flush()

    return UpdateProfileResponse.model_validate(user)


def change_password(db: Session, auth: AuthContext, req: ChangePasswordRequest) -> None:
    """비밀번호 변경.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    user = get_user_or_raise(db, auth.user_id)

    if not verify_password(req.current_password, user.hashed_password):
        raise AppError(
            message="현재 비밀번호가 올바르지 않습니다", code="INVALID_CREDENTIALS"
        )

    user.hashed_password = hash_password(req.new_password)
    db.flush()
