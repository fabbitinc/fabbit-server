"""인증 비즈니스 로직 — 이메일 인증, 토큰 관리, 초대 레코드 관리."""

from __future__ import annotations

import uuid as _uuid
from datetime import datetime, timezone
from typing import TYPE_CHECKING

from loguru import logger
from sqlalchemy.orm import Session

if TYPE_CHECKING:
    from app.core.auth_context import AuthContext

from app.core.config import settings
from app.core.exceptions import AppError
from app.infrastructure.token_provider import token_provider
from app.infrastructure.turnstile import verify_turnstile_token
from app.modules.auth import repository as repo
from app.modules.auth.constants import InvitationStatus
from app.modules.auth.models import EmailVerification, Invitation, _hash_token
from app.modules.auth.schemas import (
    SendVerificationRequest,
    SendVerificationResponse,
    TokenResponse,
    VerifyEmailRequest,
    VerifyEmailResponse,
)
from app.modules.organization.constants import MembershipRole


# ── Turnstile 검증 ──


def verify_turnstile(token: str) -> None:
    """Turnstile 봇 방지 토큰 검증."""
    verify_turnstile_token(token)


# ── 이메일 인증 ──


def send_verification_email(
    db: Session, req: SendVerificationRequest
) -> SendVerificationResponse:
    """이메일 인증코드 발송.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    # Turnstile 봇 방지 검증
    verify_turnstile_token(req.turnstile_token)

    # 이미 가입된 이메일인지 확인
    if repo.exists_user_by_email(db, req.email):
        raise AppError(message="이미 가입된 이메일입니다", code="ALREADY_EXISTS")

    # 쿨다운 체크: 최근 PENDING이 60초 이내면 에러
    existing = repo.get_pending_verification_by_email(db, req.email)
    if existing:
        elapsed = (datetime.now(timezone.utc) - existing.created_at).total_seconds()
        if elapsed < settings.email_verification_cooldown_seconds:
            raise AppError(
                message="잠시 후 다시 시도해 주세요",
                code="RATE_LIMITED",
            )

    # 기존 PENDING 삭제 (재발송)
    repo.delete_pending_verifications_by_email(db, req.email)

    # 인증코드 생성
    verification, code = EmailVerification.create(req.email)
    repo.create_email_verification(db, verification)

    # 이메일 발송
    _send_verification_code_email(req.email, code)

    return SendVerificationResponse(message="인증코드가 발송되었습니다")


def verify_email(db: Session, req: VerifyEmailRequest) -> VerifyEmailResponse:
    """인증코드 검증.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    code_hash = _hash_token(req.code)
    verification = repo.get_pending_verification_by_email_and_code_hash(
        db, req.email, code_hash
    )

    if not verification:
        # 코드 불일치 — 같은 이메일의 PENDING 레코드 찾아서 attempt_count 증가
        pending = repo.get_pending_verification_by_email(db, req.email)
        if pending:
            pending.increment_attempt()
            if pending.is_max_attempts:
                raise AppError(
                    message="인증 시도 횟수를 초과했습니다. 인증코드를 재발송해 주세요",
                    code="MAX_ATTEMPTS_EXCEEDED",
                )
        raise AppError(
            message="인증코드가 올바르지 않습니다", code="INVALID_CODE"
        )

    # 만료 확인
    if verification.is_expired:
        raise AppError(
            message="인증코드가 만료되었습니다. 재발송해 주세요",
            code="CODE_EXPIRED",
        )

    # 시도 횟수 초과 확인
    if verification.is_max_attempts:
        raise AppError(
            message="인증 시도 횟수를 초과했습니다. 인증코드를 재발송해 주세요",
            code="MAX_ATTEMPTS_EXCEEDED",
        )

    # VERIFIED + verification_token 생성
    raw_token = verification.verify()

    return VerifyEmailResponse(verification_token=raw_token, email=verification.email)


def validate_and_consume_verification(
    db: Session, verification_token: str, code: str
) -> str:
    """인증 재검증 + USED 처리 → 이메일 반환 (register use_case용).

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    token_hash = _hash_token(verification_token)
    code_hash = _hash_token(code)
    verification = repo.get_verified_by_token_hash_and_code_hash(db, token_hash, code_hash)
    if not verification:
        raise AppError(
            message="유효하지 않은 인증 정보입니다", code="INVALID_VERIFICATION"
        )
    if verification.is_expired:
        raise AppError(
            message="인증이 만료되었습니다. 다시 인증해 주세요", code="CODE_EXPIRED"
        )
    verification.use()
    return verification.email


# ── 토큰 관리 ──


def issue_tokens(
    db: Session,
    user_id: _uuid.UUID,
    email: str,
    org_id: _uuid.UUID,
    role: str,
) -> TokenResponse:
    """access + refresh 토큰 발급 + DB 저장."""
    access_token = token_provider.create_access_token(
        sub=str(user_id),
        email=email,
        org_id=str(org_id),
        role=role,
    )
    refresh_token_str, expires_at = token_provider.create_refresh_token(
        sub=str(user_id), email=email
    )
    payload = token_provider.decode(refresh_token_str)
    repo.save_refresh_token(db, user_id, payload.jti, expires_at)

    return TokenResponse(
        access_token=access_token, refresh_token=refresh_token_str
    )


def issue_scoped_token(
    user_id: _uuid.UUID, email: str, scope: str
) -> str:
    """스코프 토큰 발급 (조직 생성용 등)."""
    return token_provider.create_scoped_token(
        sub=str(user_id),
        email=email,
        scope=scope,
    )


def validate_refresh_token(
    db: Session, refresh_token_str: str
) -> tuple:
    """리프레시 토큰 검증 → (payload, stored RefreshToken) 반환.

    재사용 감지 시 모든 토큰 폐기 + commit + raise.
    """
    payload = token_provider.decode(refresh_token_str)
    if payload.token_type != "REFRESH":
        raise AppError(message="리프레시 토큰이 아닙니다", code="TOKEN_INVALID")

    stored = repo.get_refresh_token_by_jti(db, payload.jti)
    if not stored:
        # 재사용 감지 — 모든 토큰 폐기 (커밋 후 예외)
        logger.warning(
            "리프레시 토큰 재사용 감지, 전체 폐기: user={user}", user=payload.sub
        )
        repo.delete_all_user_refresh_tokens(db, _uuid.UUID(payload.sub))
        db.commit()
        raise AppError(
            message="토큰이 재사용되었습니다. 다시 로그인해주세요", code="TOKEN_INVALID"
        )

    return payload, stored


def revoke_refresh_token(db: Session, jti: str) -> None:
    """리프레시 토큰 단건 폐기."""
    repo.delete_refresh_token_by_jti(db, jti)


def revoke_all_user_tokens(db: Session, user_id: _uuid.UUID) -> None:
    """유저의 모든 리프레시 토큰 폐기."""
    repo.delete_all_user_refresh_tokens(db, user_id)


def logout(db: Session, auth: AuthContext, refresh_token_str: str) -> None:
    """로그아웃: 리프레시 토큰 폐기.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    payload = token_provider.decode(refresh_token_str)
    if payload.jti:
        repo.delete_refresh_token_by_jti(db, payload.jti)


# ── 초대 레코드 관리 ──


def create_invitation_record(
    db: Session,
    org_id: _uuid.UUID,
    email: str,
    invited_by: _uuid.UUID,
    role: str,
) -> tuple[Invitation, str]:
    """초대 레코드 생성 (검증 + DB 저장). raw_token 반환.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    # 역할 검증
    try:
        MembershipRole(role)
    except ValueError:
        raise AppError(message="유효하지 않은 역할입니다", code="VALIDATION_ERROR")

    # PENDING 초대 중복 확인
    pending = repo.get_pending_invitation(db, org_id, email)
    if pending:
        raise AppError(message="이미 초대가 발송된 이메일입니다", code="ALREADY_EXISTS")

    # 기존 CANCELLED 레코드 삭제 (재초대 허용)
    repo.delete_invitation_by_org_email(db, org_id, email)

    # 초대 생성
    invitation, raw_token = Invitation.create(
        org_id=org_id,
        email=email,
        invited_by=invited_by,
        role=role,
    )
    invitation = repo.create_invitation(db, invitation)

    return invitation, raw_token


def cancel_invitation(
    db: Session, auth: AuthContext, invitation_id: _uuid.UUID
) -> None:
    """초대 취소.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    RBAC(ADMIN 검증)은 router Depends(require_admin)에서 처리.
    """
    invitation = repo.get_invitation_by_id(db, invitation_id)
    if not invitation or invitation.org_id != auth.org_id:
        raise AppError(message="초대를 찾을 수 없습니다", code="NOT_FOUND")

    if invitation.status != InvitationStatus.PENDING:
        raise AppError(
            message="대기 중인 초대만 취소할 수 있습니다", code="VALIDATION_ERROR"
        )

    invitation.cancel()


def send_invitation_email(
    email: str, org_name: str, inviter_name: str, invite_url: str
) -> None:
    """초대 이메일 발송."""
    from app.infrastructure.email_client import email_client

    email_client.send_template(
        to=email,
        subject=f"[Fabbit] {org_name} 워크스페이스에 초대되었습니다",
        template_name="invitation",
        org_name=org_name,
        inviter_name=inviter_name,
        invite_url=invite_url,
    )


def build_invite_url(token: str, slug: str) -> str:
    """초대 수락 페이지 URL 생성 ({slug}.{base_domain} 서브도메인 패턴)."""
    from urllib.parse import urlparse

    parsed = urlparse(settings.invitation_base_url)
    port_suffix = f":{parsed.port}" if parsed.port else ""
    base = f"{parsed.scheme}://{slug}.{settings.base_domain}{port_suffix}"
    return f"{base}/invite/accept?token={token}"


def _send_verification_code_email(email: str, code: str) -> None:
    """인증코드 이메일 발송."""
    from app.infrastructure.email_client import email_client

    email_client.send_template(
        to=email,
        subject="[Fabbit] 이메일 인증코드",
        template_name="verification",
        code=code,
    )
