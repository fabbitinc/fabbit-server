"""이메일 중복 확인."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import repository as auth_repo
from app.modules.auth.schemas import CheckEmailResponse


@transactional(read_only=True)
def check_email(db: Session, email: str) -> CheckEmailResponse:
    """이메일 사용 가능 여부 확인."""
    exists = auth_repo.exists_user_by_email(db, email)
    return CheckEmailResponse(
        available=not exists,
        message="이미 가입된 이메일입니다" if exists else None,
    )
