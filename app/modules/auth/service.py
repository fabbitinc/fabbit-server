"""인증 비즈니스 로직."""

import re
import unicodedata

from loguru import logger
from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.infrastructure.password_hasher import hash_password, verify_password
from app.infrastructure.token_provider import TokenProvider
from app.modules.auth import repository as repo
from app.modules.auth.provisioning import provision_tenant
from app.modules.auth.schemas import (
    LoginRequest,
    LoginResponse,
    MeResponse,
    MembershipResponse,
    OrganizationResponse,
    SignupRequest,
    SignupResponse,
    TokenResponse,
    UserResponse,
)

token_provider = TokenProvider()


def _slugify(name: str) -> str:
    """조직명을 URL-safe slug로 변환."""
    # 유니코드 정규화 후 ASCII 변환
    name = unicodedata.normalize("NFKD", name)
    # 한글 등 non-ASCII는 유지
    slug = re.sub(r"[^\w\s-]", "", name).strip().lower()
    slug = re.sub(r"[-\s]+", "-", slug)
    return slug[:50]


def signup(db: Session, req: SignupRequest) -> SignupResponse:
    """통합 회원가입: 유저 + 조직 + 멤버십 + 테넌트 프로비저닝 + 토큰 발급."""
    # 이메일 중복 검사
    if repo.get_user_by_email(db, req.email):
        raise AppError(message="이미 가입된 이메일입니다", code="ALREADY_EXISTS")

    # slug 생성 및 중복 처리
    slug = _slugify(req.org_name)
    if not slug:
        slug = "org"
    if repo.get_org_by_slug(db, slug):
        import uuid as _uuid

        slug = f"{slug}-{str(_uuid.uuid4())[:8]}"

    # 유저 생성
    hashed = hash_password(req.password)
    user = repo.create_user(db, req.email, hashed, req.full_name)

    # 조직 생성
    org = repo.create_organization(db, slug, req.org_name, user.id)

    # 멤버십 (ADMIN)
    repo.create_membership(db, user.id, org.id, role="ADMIN")

    # 테넌트 프로비저닝 (스키마 + AGE 그래프)
    schema_name = provision_tenant(db, org.id)
    logger.info(
        "테넌트 프로비저닝 완료: {schema}", schema=schema_name, org_id=str(org.id)
    )

    # 토큰 발급
    access_token = token_provider.create_access_token(
        sub=str(user.id), email=user.email, org_id=str(org.id)
    )
    refresh_token_str, expires_at = token_provider.create_refresh_token(
        sub=str(user.id), email=user.email
    )
    # refresh token jti를 DB에 저장
    payload = token_provider.decode(refresh_token_str)
    repo.save_refresh_token(db, user.id, payload.jti, expires_at)

    db.commit()

    return SignupResponse(
        user=UserResponse.model_validate(user),
        organization=OrganizationResponse.model_validate(org),
        tokens=TokenResponse(
            access_token=access_token, refresh_token=refresh_token_str
        ),
    )


def login(db: Session, req: LoginRequest) -> LoginResponse:
    """로그인: 자격증명 검증 + 토큰 발급."""
    user = repo.get_user_by_email(db, req.email)
    if not user or not verify_password(req.password, user.hashed_password):
        raise AppError(message="이메일 또는 비밀번호가 올바르지 않습니다", code="INVALID_CREDENTIALS")

    if not user.is_active:
        raise AppError(message="비활성화된 계정입니다", code="FORBIDDEN")

    # 첫 번째 소속 조직을 기본 org_id로 사용
    memberships = repo.get_user_memberships(db, user.id)
    if not memberships:
        raise AppError(message="소속된 조직이 없습니다", code="FORBIDDEN")

    access_token = token_provider.create_access_token(
        sub=str(user.id), email=user.email, org_id=str(memberships[0].org_id)
    )
    refresh_token_str, expires_at = token_provider.create_refresh_token(
        sub=str(user.id), email=user.email
    )
    payload = token_provider.decode(refresh_token_str)
    repo.save_refresh_token(db, user.id, payload.jti, expires_at)

    db.commit()

    return LoginResponse(
        user=UserResponse.model_validate(user),
        tokens=TokenResponse(
            access_token=access_token, refresh_token=refresh_token_str
        ),
    )


def refresh_tokens(db: Session, refresh_token_str: str) -> TokenResponse:
    """토큰 갱신 (회전): 기존 jti 삭제 → 새 토큰 발급.

    재사용 감지: DB에 없는 jti로 요청 시 해당 유저의 모든 토큰 폐기.
    """
    payload = token_provider.decode(refresh_token_str)
    if payload.token_type != "REFRESH":
        raise AppError(message="리프레시 토큰이 아닙니다", code="TOKEN_INVALID")

    stored = repo.get_refresh_token_by_jti(db, payload.jti)
    if not stored:
        # 재사용 감지 — 모든 토큰 폐기
        logger.warning(
            "리프레시 토큰 재사용 감지, 전체 폐기: user={user}", user=payload.sub
        )
        import uuid

        repo.delete_all_user_refresh_tokens(db, uuid.UUID(payload.sub))
        db.commit()
        raise AppError(message="토큰이 재사용되었습니다. 다시 로그인해주세요", code="TOKEN_INVALID")

    # 기존 토큰 삭제 (회전)
    repo.delete_refresh_token_by_jti(db, payload.jti)

    # 새 토큰 발급
    user = repo.get_user_by_id(db, stored.user_id)
    if not user:
        raise AppError(message="사용자를 찾을 수 없습니다", code="NOT_FOUND")

    memberships = repo.get_user_memberships(db, user.id)
    if not memberships:
        raise AppError(message="소속된 조직이 없습니다", code="FORBIDDEN")

    new_access = token_provider.create_access_token(
        sub=str(user.id), email=user.email, org_id=str(memberships[0].org_id)
    )
    new_refresh_str, new_expires = token_provider.create_refresh_token(
        sub=str(user.id), email=user.email
    )
    new_payload = token_provider.decode(new_refresh_str)
    repo.save_refresh_token(db, user.id, new_payload.jti, new_expires)

    db.commit()

    return TokenResponse(access_token=new_access, refresh_token=new_refresh_str)


def logout(db: Session, user_id: str, refresh_token_str: str) -> None:
    """로그아웃: 리프레시 토큰 폐기."""
    payload = token_provider.decode(refresh_token_str)
    if payload.jti:
        repo.delete_refresh_token_by_jti(db, payload.jti)
    db.commit()


def get_me(db: Session, user_id: str) -> MeResponse:
    """현재 유저 + 소속 조직 목록."""
    import uuid

    user = repo.get_user_by_id(db, uuid.UUID(user_id))
    if not user:
        raise AppError(message="사용자를 찾을 수 없습니다", code="NOT_FOUND")

    memberships = repo.get_user_memberships(db, user.id)

    return MeResponse(
        user=UserResponse.model_validate(user),
        memberships=[
            MembershipResponse(
                org_id=m.org_id,
                role=m.role,
                organization=OrganizationResponse.model_validate(m.organization),
            )
            for m in memberships
        ],
    )
