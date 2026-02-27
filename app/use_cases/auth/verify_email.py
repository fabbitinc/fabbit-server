"""인증코드 검증 — 코드 확인 + verification_token 발급."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import VerifyEmailRequest, VerifyEmailResponse


@transactional()
def verify_email(
    db: Session,
    req: VerifyEmailRequest,
) -> VerifyEmailResponse:
    """인증코드 검증."""
    return auth_service.verify_email(db, req)
