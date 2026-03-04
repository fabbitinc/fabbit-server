"""사용자 비즈니스 로직."""

from __future__ import annotations

import uuid
from typing import TYPE_CHECKING

from sqlalchemy.orm import Session

if TYPE_CHECKING:
    from app.core.auth_context import AuthContext
    from app.modules.file.models import File
    from app.modules.user.schemas import (
        ChangePasswordRequest,
        UpdateProfileRequest,
        UpdateProfileResponse,
    )

from app.core.exceptions import AppError
from app.infrastructure.password_hasher import hash_password, verify_password
from app.modules.file.mapper import get_file_url
from app.modules.user import repository as repo
from app.modules.user.models import User


def create_user(db: Session, email: str, password: str, full_name: str) -> User:
    """유저 생성 (비밀번호 해싱 포함).

    register/accept_invitation use_case에서 호출.
    """
    hashed = hash_password(password)
    return repo.create_user(db, email, hashed, full_name)


def find_or_create_for_invitation(
    db: Session,
    email: str,
    password: str | None,
    full_name: str | None,
) -> tuple[User, bool]:
    """이메일로 기존 유저 조회, 없으면 생성. (User, is_new_user) 반환.

    초대 수락 use_case에서 호출. 미가입자는 password/full_name 필수.
    """
    user = repo.get_user_by_email(db, email)
    if user:
        return user, False

    if not password or not full_name:
        raise AppError(
            message="신규 가입 시 비밀번호와 이름이 필요합니다",
            code="VALIDATION_ERROR",
        )
    return create_user(db, email, password, full_name), True


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


def get_user_by_email(db: Session, email: str) -> User | None:
    """이메일로 유저 조회. 없으면 None."""
    return repo.get_user_by_email(db, email)


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
    user = get_user_or_raise(db, auth.user_id)

    updates = req.model_dump(exclude_unset=True)
    if not updates:
        return _to_update_profile_response(user)

    for field, value in updates.items():
        setattr(user, field, value)
    db.flush()

    return _to_update_profile_response(user)


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


def set_profile_image(db: Session, auth: AuthContext, file: File) -> None:
    """프로필 이미지 설정 — 검증된 파일을 연결.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    user = get_user_or_raise(db, auth.user_id)
    user.set_profile_image(file, auth.org_id)


def delete_profile_image(
    db: Session, auth: AuthContext, file_id: uuid.UUID
) -> None:
    """프로필 이미지 제거 — 소프트 삭제는 FileHandler가 처리.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    user = get_user_or_raise(db, auth.user_id)
    user.remove_profile_image(file_id, auth.org_id)


def _to_update_profile_response(user: User) -> UpdateProfileResponse:
    """User → UpdateProfileResponse 변환 (profile_image_url 포함)."""
    from app.modules.user.schemas import UpdateProfileResponse

    return UpdateProfileResponse(
        id=user.id,
        email=user.email,
        full_name=user.full_name,
        phone=user.phone,
        profile_image_url=get_file_url(user.profile_image_file_key),
        updated_at=user.updated_at,
    )
