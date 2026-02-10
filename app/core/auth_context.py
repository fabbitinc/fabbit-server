"""인증 컨텍스트 - 요청 스코프 인증 정보 관리."""

import uuid
from contextvars import ContextVar, Token
from dataclasses import dataclass


@dataclass(frozen=True)
class AuthContext:
    """요청 스코프 인증 컨텍스트."""

    account_id: uuid.UUID
    email: str
    org_id: uuid.UUID


_auth_context: ContextVar[AuthContext | None] = ContextVar("auth_context", default=None)


def get_auth_context() -> AuthContext:
    """현재 인증 컨텍스트 조회. 설정되지 않으면 RuntimeError."""
    ctx = _auth_context.get()
    if ctx is None:
        raise RuntimeError("AuthContext is not set")
    return ctx


def get_auth_context_or_none() -> AuthContext | None:
    """현재 인증 컨텍스트 조회. 설정되지 않으면 None."""
    return _auth_context.get()


def set_auth_context(ctx: AuthContext) -> Token:
    """인증 컨텍스트 설정."""
    return _auth_context.set(ctx)


def reset_auth_context(token: Token) -> None:
    """인증 컨텍스트 복구."""
    _auth_context.reset(token)


def clear_auth_context() -> None:
    """인증 컨텍스트 초기화."""
    _auth_context.set(None)
