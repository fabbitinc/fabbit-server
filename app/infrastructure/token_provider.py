"""JWT 토큰 생성/검증."""

import uuid
from datetime import datetime, timedelta, timezone
from dataclasses import dataclass

import jwt

from app.core.config import settings
from app.core.exceptions import AppError


@dataclass
class TokenPayload:
    """디코딩된 토큰 페이로드."""

    sub: str
    email: str
    token_type: str = "ACCESS"
    org_id: str | None = None


class TokenProvider:
    """PyJWT 기반 토큰 생성/검증."""

    def create_access_token(self, sub: str, email: str, org_id: str | None = None) -> str:
        """Access Token 생성 (기본 15분 TTL)."""
        expire = datetime.now(timezone.utc) + timedelta(
            minutes=settings.access_token_expire_minutes
        )
        payload: dict = {
            "sub": sub,
            "email": email,
            "exp": expire,
            "type": "ACCESS",
            "iss": settings.jwt_issuer,
        }
        if org_id is not None:
            payload["orgId"] = org_id
        return jwt.encode(
            payload, settings.jwt_secret_key, algorithm=settings.jwt_algorithm
        )

    def create_refresh_token(self, sub: str, email: str) -> tuple[str, datetime]:
        """Refresh Token 생성 (기본 7일 TTL, jti 포함)."""
        expires_at = datetime.now(timezone.utc) + timedelta(
            days=settings.refresh_token_expire_days
        )
        payload: dict = {
            "sub": sub,
            "email": email,
            "exp": expires_at,
            "type": "REFRESH",
            "jti": str(uuid.uuid4()),
            "iss": settings.jwt_issuer,
        }
        return (
            jwt.encode(
                payload, settings.jwt_secret_key, algorithm=settings.jwt_algorithm
            ),
            expires_at,
        )

    def decode(self, token: str) -> TokenPayload:
        """토큰 검증 + payload 파싱."""
        try:
            payload = jwt.decode(
                token,
                settings.jwt_secret_key,
                algorithms=[settings.jwt_algorithm],
                issuer=settings.jwt_issuer,
            )
            return TokenPayload(
                sub=payload["sub"],
                email=payload["email"],
                token_type=payload.get("type", "ACCESS"),
                org_id=payload.get("orgId"),
            )
        except jwt.ExpiredSignatureError:
            raise AppError(message="토큰이 만료되었습니다", code="TOKEN_EXPIRED")
        except jwt.InvalidTokenError:
            raise AppError(message="유효하지 않은 토큰입니다", code="TOKEN_INVALID")
