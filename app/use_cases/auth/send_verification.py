"""이메일 인증코드 발송 — 인증코드 생성 + 이메일 발송."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import SendVerificationRequest, SendVerificationResponse


@transactional()
def send_verification(
    db: Session,
    req: SendVerificationRequest,
) -> SendVerificationResponse:
    """이메일 인증코드 발송."""
    return auth_service.send_verification_email(db, req)
