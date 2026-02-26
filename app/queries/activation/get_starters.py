"""Activation 시작 질문 조회."""

from app.modules.activation.constants import DEFAULT_STARTERS
from app.modules.activation.schemas import StartersResponse


def get_starters() -> StartersResponse:
    """기본 시작 질문 목록을 반환합니다."""
    return StartersResponse(starters=DEFAULT_STARTERS)
