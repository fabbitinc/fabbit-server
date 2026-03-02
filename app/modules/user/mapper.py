"""User 도메인 모델 → Pydantic 응답 변환."""

from app.modules.file.mapper import get_file_url
from app.modules.user.models import User
from app.modules.user.schemas import UserSummary


def to_user_summary(user: User) -> UserSummary:
    """User 모델 → UserSummary 변환."""
    return UserSummary(
        user_id=user.id,
        full_name=user.full_name,
        email=user.email,
        phone=user.phone,
        profile_image_url=get_file_url(user.profile_image_file_key),
    )
