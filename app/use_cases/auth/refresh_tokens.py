"""토큰 갱신 — 회전 + 재사용 감지."""

from sqlalchemy.orm import Session

from app.modules.auth import service as auth_service
from app.modules.auth.schemas import TokenResponse
from app.modules.organization import service as org_service
from app.modules.user import service as user_service


def refresh_tokens(db: Session, refresh_token_str: str) -> TokenResponse:
    """토큰 갱신 (회전): 기존 jti 삭제 → 새 토큰 발급.

    재사용 감지: DB에 없는 jti로 요청 시 해당 유저의 모든 토큰 폐기.

    Note: @transactional 미적용 — 재사용 감지 시 삭제 커밋 후 예외를 발생시켜야 하므로
    데코레이터의 "예외 시 rollback" 정책과 충돌합니다. 수동 try/except로 보호합니다.
    """
    # 1. 리프레시 토큰 검증 (재사용 감지 시 commit → raise)
    payload, stored = auth_service.validate_refresh_token(db, refresh_token_str)

    # 2. 정상 경로 — 기존 토큰 삭제 + 새 토큰 발급
    try:
        auth_service.revoke_refresh_token(db, payload.jti)
        user = user_service.get_user_or_raise(db, stored.user_id)
        membership = org_service.get_first_membership_or_raise(db, user.id)
        tokens = auth_service.issue_tokens(
            db, user.id, user.email, membership.org_id, membership.role
        )

        db.commit()
    except Exception:
        db.rollback()
        raise

    return tokens
