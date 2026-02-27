"""인증 비즈니스 로직."""

import re
import unicodedata
import uuid as _uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.password_hasher import hash_password, verify_password
from app.infrastructure.turnstile import verify_turnstile_token
from app.infrastructure.token_provider import token_provider
from app.modules.auth import repository as repo
from app.modules.auth.constants import PLAN_LIMITS, InvitationStatus, MembershipRole, PlanType, RESERVED_SLUGS, validate_slug_format
from app.modules.auth.provisioning import provision_tenant
from app.modules.auth.models import Invitation, _hash_token
from app.modules.auth.schemas import (
    AcceptInvitationRequest,
    AcceptInvitationResponse,
    CheckEmailResponse,
    CheckSlugResponse,
    CreateInvitationRequest,
    InvitationResponse,
    LoginRequest,
    LoginResponse,
    MeResponse,
    MembershipResponse,
    OrganizationResponse,
    PlanResponse,
    RegisterRequest,
    RegisterResponse,
    SiteResponse,
    TokenResponse,
    UserResponse,
)



def _slugify(name: str) -> str:
    """조직명을 URL-safe slug로 변환."""
    # 유니코드 정규화 후 ASCII 변환
    name = unicodedata.normalize("NFKD", name)
    # 한글 등 non-ASCII는 유지
    slug = re.sub(r"[^\w\s-]", "", name).strip().lower()
    slug = re.sub(r"[-\s]+", "-", slug)
    return slug[:50]


@transactional(read_only=True)
def check_email(db: Session, email: str) -> CheckEmailResponse:
    """이메일 중복 확인."""
    exists = repo.get_user_by_email(db, email) is not None
    return CheckEmailResponse(
        available=not exists,
        message="이미 가입된 이메일입니다" if exists else None,
    )


@transactional(read_only=True)
def check_slug(db: Session, slug: str) -> CheckSlugResponse:
    """slug 사용 가능 여부 확인."""
    # 1. 포맷 검증
    error = validate_slug_format(slug)
    if error:
        return CheckSlugResponse(available=False, message=error)
    # 2. DB 중복 확인
    if repo.get_org_by_slug(db, slug):
        suggestion = f"{slug}-{str(_uuid.uuid4())[:4]}"
        return CheckSlugResponse(
            available=False,
            message="이미 사용 중인 워크스페이스 주소입니다",
            suggestion=suggestion,
        )
    return CheckSlugResponse(available=True)


@transactional
def register(db: Session, req: RegisterRequest) -> RegisterResponse:
    """통합 회원가입: 유저 + 조직 + 멤버십 + 테넌트 프로비저닝 + 토큰 발급."""
    # Turnstile 봇 방지 검증
    verify_turnstile_token(req.turnstile_token)

    # 플랜 검증
    try:
        plan = PlanType(req.plan_type)
    except ValueError:
        raise AppError(message="유효하지 않은 플랜입니다", code="VALIDATION_ERROR")

    # 이메일 중복 검사
    if repo.get_user_by_email(db, req.email):
        raise AppError(message="이미 가입된 이메일입니다", code="ALREADY_EXISTS")

    # slug 결정
    if req.slug:
        # 커스텀 slug: 포맷 검증 + 중복 검사
        error = validate_slug_format(req.slug)
        if error:
            raise AppError(message=error, code="VALIDATION_ERROR")
        if repo.get_org_by_slug(db, req.slug):
            raise AppError(message="이미 사용 중인 워크스페이스 주소입니다", code="ALREADY_EXISTS")
        slug = req.slug
    else:
        # 자동 생성: org_name → slug
        slug = _slugify(req.org_name)
        if not slug:
            slug = "org"
        # 예약어 또는 DB 중복 시 suffix 추가
        if slug in RESERVED_SLUGS or repo.get_org_by_slug(db, slug):
            slug = f"{slug}-{str(_uuid.uuid4())[:8]}"

    # 유저 생성
    hashed = hash_password(req.password)
    user = repo.create_user(db, req.email, hashed, req.full_name)

    # 조직 생성
    org = repo.create_organization(
        db, slug, req.org_name, user.id,
        industry=req.industry, team_size=req.team_size, plan_type=plan.value,
    )

    # 멤버십 (ADMIN)
    repo.create_membership(db, user.id, org.id, role=MembershipRole.ADMIN, job_role=req.job_role)

    # 테넌트 프로비저닝 (스키마 + AGE 그래프)
    schema_name = provision_tenant(db, org.id)
    logger.info(
        "테넌트 프로비저닝 완료: {schema}", schema=schema_name, org_id=str(org.id)
    )

    # 토큰 발급
    access_token = token_provider.create_access_token(
        sub=str(user.id), email=user.email, org_id=str(org.id),
        role=MembershipRole.ADMIN,
    )
    refresh_token_str, expires_at = token_provider.create_refresh_token(
        sub=str(user.id), email=user.email
    )
    # refresh token jti를 DB에 저장
    payload = token_provider.decode(refresh_token_str)
    repo.save_refresh_token(db, user.id, payload.jti, expires_at)

    return RegisterResponse(
        user=UserResponse.model_validate(user),
        organization=OrganizationResponse.model_validate(org),
        tokens=TokenResponse(
            access_token=access_token, refresh_token=refresh_token_str
        ),
    )


@transactional
def login(db: Session, req: LoginRequest, *, slug: str | None = None) -> LoginResponse:
    """로그인: 자격증명 검증 + 토큰 발급."""
    user = repo.get_user_by_email(db, req.email)
    if not user or not verify_password(req.password, user.hashed_password):
        raise AppError(message="이메일 또는 비밀번호가 올바르지 않습니다", code="INVALID_CREDENTIALS")

    if not user.is_active:
        raise AppError(message="비활성화된 계정입니다", code="FORBIDDEN")

    # Origin 서브도메인에서 추출한 slug로 조직 결정
    if not slug:
        raise AppError(message="워크스페이스를 통해 로그인해주세요", code="VALIDATION_ERROR")

    membership = repo.get_membership_by_slug(db, user.id, slug)
    if not membership:
        raise AppError(message="해당 워크스페이스에 소속되어 있지 않습니다", code="FORBIDDEN")
    org_id = membership.org_id

    access_token = token_provider.create_access_token(
        sub=str(user.id), email=user.email, org_id=str(org_id),
        role=membership.role,
    )
    refresh_token_str, expires_at = token_provider.create_refresh_token(
        sub=str(user.id), email=user.email
    )
    payload = token_provider.decode(refresh_token_str)
    repo.save_refresh_token(db, user.id, payload.jti, expires_at)

    return LoginResponse(
        user=UserResponse.model_validate(user),
        tokens=TokenResponse(
            access_token=access_token, refresh_token=refresh_token_str
        ),
    )


def refresh_tokens(db: Session, refresh_token_str: str) -> TokenResponse:
    """토큰 갱신 (회전): 기존 jti 삭제 → 새 토큰 발급.

    재사용 감지: DB에 없는 jti로 요청 시 해당 유저의 모든 토큰 폐기.

    Note: @transactional 미적용 — 재사용 감지 시 삭제 커밋 후 예외를 발생시켜야 하므로
    데코레이터의 "예외 시 rollback" 정책과 충돌합니다. 수동 try/except로 보호합니다.
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
        import uuid

        repo.delete_all_user_refresh_tokens(db, uuid.UUID(payload.sub))
        db.commit()
        raise AppError(message="토큰이 재사용되었습니다. 다시 로그인해주세요", code="TOKEN_INVALID")

    # 정상 경로 — 기존 토큰 삭제 + 새 토큰 발급
    try:
        repo.delete_refresh_token_by_jti(db, payload.jti)

        user = repo.get_user_by_id(db, stored.user_id)
        if not user:
            raise AppError(message="사용자를 찾을 수 없습니다", code="NOT_FOUND")

        memberships = repo.get_user_memberships(db, user.id)
        if not memberships:
            raise AppError(message="소속된 조직이 없습니다", code="FORBIDDEN")

        membership = memberships[0]
        new_access = token_provider.create_access_token(
            sub=str(user.id), email=user.email,
            org_id=str(membership.org_id), role=membership.role,
        )
        new_refresh_str, new_expires = token_provider.create_refresh_token(
            sub=str(user.id), email=user.email
        )
        new_payload = token_provider.decode(new_refresh_str)
        repo.save_refresh_token(db, user.id, new_payload.jti, new_expires)

        db.commit()
    except Exception:
        db.rollback()
        raise

    return TokenResponse(access_token=new_access, refresh_token=new_refresh_str)


@transactional
def logout(db: Session, auth: AuthContext, refresh_token_str: str) -> None:
    """로그아웃: 리프레시 토큰 폐기."""
    payload = token_provider.decode(refresh_token_str)
    if payload.jti:
        repo.delete_refresh_token_by_jti(db, payload.jti)


@transactional(read_only=True)
def get_me(db: Session, auth: AuthContext) -> MeResponse:
    """현재 유저 + 소속 조직 목록."""
    user = repo.get_user_by_id(db, auth.user_id)
    if not user:
        raise AppError(message="사용자를 찾을 수 없습니다", code="NOT_FOUND")

    memberships = repo.get_user_memberships(db, user.id)

    return MeResponse(
        user=UserResponse.model_validate(user),
        memberships=[
            MembershipResponse(
                org_id=m.org_id,
                role=m.role,
                job_role=m.job_role,
                organization=OrganizationResponse.model_validate(m.organization),
            )
            for m in memberships
        ],
    )


@transactional
def complete_onboarding(db: Session, auth: AuthContext) -> OrganizationResponse:
    """조직 온보딩 완료 처리."""
    org = repo.get_org_by_id(db, auth.org_id)
    if not org:
        raise AppError(message="조직을 찾을 수 없습니다", code="NOT_FOUND")
    if org.onboarded_at:
        raise AppError(message="이미 온보딩이 완료된 조직입니다", code="ALREADY_EXISTS")

    org = repo.complete_onboarding(db, auth.org_id)
    return OrganizationResponse.model_validate(org)


@transactional(read_only=True)
def get_site(db: Session, slug: str | None) -> SiteResponse:
    """서브도메인 slug로 워크스페이스 기본 정보 조회."""
    if not slug:
        raise AppError(message="워크스페이스를 통해 접근해주세요", code="VALIDATION_ERROR")
    org = repo.get_org_by_slug(db, slug)
    if not org:
        raise AppError(message="존재하지 않는 워크스페이스입니다", code="NOT_FOUND")
    return SiteResponse.model_validate(org)


def get_plans() -> list[PlanResponse]:
    """플랜 목록 및 제한값 조회."""
    return [
        PlanResponse(
            plan_type=pt.value,
            display_name=limits.display_name,
            description=limits.description,
            storage_gb=limits.storage_gb,
            max_bom=limits.max_bom,
            max_drawing_parses=limits.max_drawing_parses,
            max_chats=limits.max_chats,
            price_monthly=limits.price_monthly,
        )
        for pt, limits in PLAN_LIMITS.items()
    ]


# ── 초대 ──


def _build_invite_url(token: str) -> str:
    """초대 수락 페이지 URL 생성."""
    from app.core.config import settings
    return f"{settings.invitation_base_url}/invite/accept?token={token}"


def _send_invitation_email(
    email: str, org_name: str, inviter_name: str, invite_url: str
) -> None:
    """초대 이메일 발송."""
    from app.infrastructure.email_client import email_client

    subject = f"[Fabbit] {org_name} 워크스페이스에 초대되었습니다"
    html_body = f"""\
<div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
  <h2>{org_name} 워크스페이스 초대</h2>
  <p><strong>{inviter_name}</strong>님이 <strong>{org_name}</strong> 워크스페이스에 초대했습니다.</p>
  <p>아래 버튼을 클릭하여 초대를 수락하세요.</p>
  <p style="margin: 24px 0;">
    <a href="{invite_url}"
       style="background-color: #2563eb; color: white; padding: 12px 24px;
              text-decoration: none; border-radius: 6px; display: inline-block;">
      초대 수락하기
    </a>
  </p>
  <p style="color: #6b7280; font-size: 14px;">
    버튼이 작동하지 않으면 아래 링크를 브라우저에 복사하세요:<br>
    <a href="{invite_url}">{invite_url}</a>
  </p>
</div>"""
    text_body = (
        f"{inviter_name}님이 {org_name} 워크스페이스에 초대했습니다.\n"
        f"초대 수락: {invite_url}"
    )
    email_client.send(email, subject, html_body, text_body)


def create_invitation(
    db: Session, auth: AuthContext, req: CreateInvitationRequest
) -> InvitationResponse:
    """초대 생성 및 이메일 발송."""
    # ADMIN 검증
    if auth.role != MembershipRole.ADMIN:
        raise AppError(message="관리자만 초대할 수 있습니다", code="FORBIDDEN")

    # 역할 검증
    try:
        MembershipRole(req.role)
    except ValueError:
        raise AppError(message="유효하지 않은 역할입니다", code="VALIDATION_ERROR")

    # 이미 조직 멤버인지 확인
    existing_user = repo.get_user_by_email(db, req.email)
    if existing_user:
        existing_membership = repo.get_membership(db, existing_user.id, auth.org_id)
        if existing_membership:
            raise AppError(message="이미 조직에 소속된 멤버입니다", code="ALREADY_EXISTS")

    # PENDING 초대 중복 확인
    pending = repo.get_pending_invitation(db, auth.org_id, req.email)
    if pending:
        raise AppError(message="이미 초대가 발송된 이메일입니다", code="ALREADY_EXISTS")

    # 기존 CANCELLED 레코드 삭제 (재초대 허용)
    repo.delete_invitation_by_org_email(db, auth.org_id, req.email)

    # 초대 생성 (원본 토큰은 이메일 발송에만 사용)
    invitation, raw_token = Invitation.create(
        org_id=auth.org_id,
        email=req.email,
        invited_by=auth.user_id,
        role=req.role,
    )
    invitation = repo.create_invitation(db, invitation)

    # 이메일 발송
    org = repo.get_org_by_id(db, auth.org_id)
    inviter = repo.get_user_by_id(db, auth.user_id)
    invite_url = _build_invite_url(raw_token)
    _send_invitation_email(req.email, org.name, inviter.full_name, invite_url)

    return InvitationResponse.model_validate(invitation)


def cancel_invitation(
    db: Session, auth: AuthContext, invitation_id: _uuid.UUID
) -> None:
    """초대 취소."""
    if auth.role != MembershipRole.ADMIN:
        raise AppError(message="관리자만 취소할 수 있습니다", code="FORBIDDEN")

    invitation = repo.get_invitation_by_id(db, invitation_id)
    if not invitation or invitation.org_id != auth.org_id:
        raise AppError(message="초대를 찾을 수 없습니다", code="NOT_FOUND")

    if invitation.status != InvitationStatus.PENDING:
        raise AppError(message="대기 중인 초대만 취소할 수 있습니다", code="VALIDATION_ERROR")

    invitation.cancel()


def accept_invitation(
    db: Session, req: AcceptInvitationRequest
) -> AcceptInvitationResponse:
    """초대 수락: 미가입 시 User 생성 → Membership 생성 → 토큰 발급."""
    token_hash = _hash_token(req.token)
    invitation = repo.get_invitation_by_token_hash(db, token_hash)
    if not invitation:
        raise AppError(message="유효하지 않은 초대입니다", code="NOT_FOUND")

    if invitation.status != InvitationStatus.PENDING:
        raise AppError(message="이미 처리된 초대입니다", code="VALIDATION_ERROR")

    if invitation.is_expired:
        raise AppError(message="만료된 초대입니다", code="VALIDATION_ERROR")

    # 기존 유저 확인
    user = repo.get_user_by_email(db, invitation.email)
    is_new_user = user is None

    if is_new_user:
        # 미가입자 — password, full_name 필수
        if not req.password or not req.full_name:
            raise AppError(
                message="신규 가입 시 비밀번호와 이름이 필요합니다",
                code="VALIDATION_ERROR",
            )
        hashed = hash_password(req.password)
        user = repo.create_user(db, invitation.email, hashed, req.full_name)

    # 이미 멤버인지 확인
    existing_membership = repo.get_membership(db, user.id, invitation.org_id)
    if existing_membership:
        invitation.accept()
        raise AppError(message="이미 조직에 소속된 멤버입니다", code="ALREADY_EXISTS")

    # 멤버십 생성
    repo.create_membership(db, user.id, invitation.org_id, role=invitation.role)

    # 초대 수락 처리
    invitation.accept()

    # 토큰 발급
    org = repo.get_org_by_id(db, invitation.org_id)
    access_token = token_provider.create_access_token(
        sub=str(user.id), email=user.email,
        org_id=str(invitation.org_id), role=invitation.role,
    )
    refresh_token_str, expires_at = token_provider.create_refresh_token(
        sub=str(user.id), email=user.email
    )
    payload = token_provider.decode(refresh_token_str)
    repo.save_refresh_token(db, user.id, payload.jti, expires_at)

    return AcceptInvitationResponse(
        user=UserResponse.model_validate(user),
        organization=OrganizationResponse.model_validate(org),
        tokens=TokenResponse(
            access_token=access_token, refresh_token=refresh_token_str
        ),
        is_new_user=is_new_user,
    )
