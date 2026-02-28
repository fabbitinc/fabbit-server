"""로그인 — 자격증명 검증 + 토큰 발급."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.auth import service as auth_service
from app.modules.auth.schemas import LoginRequest, LoginResponse, ScopedLoginResponse
from app.modules.organization import service as org_service
from app.modules.user import service as user_service
from app.modules.user.schemas import UserResponse


@transactional()
def login(
    db: Session, req: LoginRequest, *, slug: str | None = None
) -> LoginResponse | ScopedLoginResponse:
    """로그인: 자격증명 검증 + 토큰 발급.

    - slug 있음: 해당 워크스페이스 멤버십 확인 → 정상 access+refresh 토큰 발급
    - slug 없음: 유저 인증만 → 조직 생성 전용 스코프 토큰 발급
    """
    # 1. 자격증명 검증
    user = user_service.authenticate(db, req.email, req.password)

    # 2-a. slug 없음 → 스코프 토큰 발급
    if not slug:
        scoped_token = auth_service.issue_scoped_token(
            user.id, user.email, "create_org"
        )
        return ScopedLoginResponse(
            user=UserResponse.model_validate(user),
            scoped_token=scoped_token,
        )

    # 2-b. slug 있음 → 멤버십 확인 + 정상 토큰 발급
    membership = org_service.switch_org(db, user.id, slug)
    tokens = auth_service.issue_tokens(
        db, user.id, user.email, membership.org_id, membership.role
    )

    return LoginResponse(
        user=UserResponse.model_validate(user),
        tokens=tokens,
    )
