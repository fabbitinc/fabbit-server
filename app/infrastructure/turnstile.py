"""Cloudflare Turnstile 토큰 검증."""

import httpx
from loguru import logger

from app.core.config import settings
from app.core.exceptions import AppError

SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify"


def verify_turnstile_token(token: str | None) -> None:
    """Turnstile 토큰 검증. 비활성화 상태 또는 DEBUG 모드면 스킵."""
    if not settings.turnstile_enabled or settings.debug:
        return

    if not token:
        raise AppError(message="Turnstile 토큰이 필요합니다", code="TURNSTILE_REQUIRED")

    resp = httpx.post(
        SITEVERIFY_URL,
        data={
            "secret": settings.turnstile_secret_key,
            "response": token,
        },
        timeout=5.0,
    )
    result = resp.json()

    if not result.get("success"):
        logger.warning(
            "Turnstile 검증 실패: {codes}",
            codes=result.get("error-codes", []),
        )
        raise AppError(message="봇 방지 검증에 실패했습니다", code="TURNSTILE_FAILED")
