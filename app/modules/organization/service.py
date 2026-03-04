"""조직 관리 비즈니스 로직."""

from __future__ import annotations

import re
import unicodedata
import uuid as _uuid
from typing import TYPE_CHECKING

from loguru import logger
from sqlalchemy.orm import Session

if TYPE_CHECKING:
    from app.core.auth_context import AuthContext
    from app.modules.file.models import File

from app.core.exceptions import AppError
from app.modules.organization import repository as repo
from app.modules.organization.constants import (
    AI_CREDIT_COSTS,
    PLAN_LIMITS,
    RESERVED_SLUGS,
    MembershipRole,
    PlanType,
    can_manage_role,
    validate_slug_format,
)
from app.modules.organization.models import Membership, Organization
from app.modules.organization.provisioning import provision_tenant
from app.modules.organization.schemas import CreateOrganizationRequest


def _slugify(name: str) -> str:
    """조직명을 URL-safe slug로 변환."""
    # 유니코드 정규화 후 ASCII 변환
    name = unicodedata.normalize("NFKD", name)
    # 한글 등 non-ASCII는 유지
    slug = re.sub(r"[^\w\s-]", "", name).strip().lower()
    slug = re.sub(r"[-\s]+", "-", slug)
    return slug[:50]


def create_organization(
    db: Session, user_id: _uuid.UUID, req: CreateOrganizationRequest
) -> Organization:
    """조직 생성 + 멤버십(OWNER) + 프로비저닝 (토큰 발급 제외).

    @transactional 없음 — use_case에서 트랜잭션 관리.
    Returns: Organization 모델 (use_case에서 토큰 발급 시 사용).
    """
    # 플랜 검증
    try:
        plan = PlanType(req.plan_type)
    except ValueError:
        raise AppError(message="유효하지 않은 플랜입니다", code="VALIDATION_ERROR")

    # slug 결정
    if req.slug:
        error = validate_slug_format(req.slug)
        if error:
            raise AppError(message=error, code="VALIDATION_ERROR")
        if repo.get_org_by_slug(db, req.slug):
            raise AppError(
                message="이미 사용 중인 워크스페이스 주소입니다", code="ALREADY_EXISTS"
            )
        slug = req.slug
    else:
        slug = _slugify(req.org_name)
        if not slug:
            slug = "org"
        if slug in RESERVED_SLUGS or repo.get_org_by_slug(db, slug):
            slug = f"{slug}-{str(_uuid.uuid4())[:8]}"

    # 실행 상태 초기값
    limits = PLAN_LIMITS[plan]

    # 조직 생성
    org = repo.create_organization(
        db,
        slug,
        req.org_name,
        user_id,
        industry=req.industry,
        team_size=req.team_size,
        plan_type=plan.value,
        max_members=limits.max_members,
        plan_credits_remaining=limits.ai_credits,
        storage_mb_limit=limits.storage_gb * 1_000,
    )

    # 멤버십 (OWNER)
    repo.create_membership(db, user_id, org.id, role=MembershipRole.OWNER)

    # 테넌트 프로비저닝
    schema_name = provision_tenant(db, org.id)
    logger.info(
        "테넌트 프로비저닝 완료: {schema}", schema=schema_name, org_id=str(org.id)
    )

    return org


def switch_org(
    db: Session, user_id: _uuid.UUID, slug: str
) -> Membership:
    """조직 전환: 대상 조직 멤버십 확인만 반환 (토큰 발급은 use_case).

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    membership = repo.get_membership_by_slug(db, user_id, slug)
    if not membership:
        raise AppError(
            message="해당 워크스페이스에 소속되어 있지 않습니다", code="FORBIDDEN"
        )
    return membership


def remove_member(
    db: Session, auth: AuthContext, user_id: _uuid.UUID
) -> None:
    """조직에서 멤버 제거.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    RBAC(ADMIN 이상 검증)은 router Depends(require_admin)에서 처리.
    """
    if auth.user_id == user_id:
        raise AppError(message="자신을 제거할 수 없습니다", code="VALIDATION_ERROR")

    membership = repo.get_membership(db, user_id, auth.org_id)
    if not membership:
        raise AppError(message="해당 멤버를 찾을 수 없습니다", code="NOT_FOUND")

    # 역할 계층 권한 체크
    actor_role = MembershipRole(auth.role)
    target_role = MembershipRole(membership.role)
    if not can_manage_role(actor_role, target_role):
        raise AppError(
            message="해당 멤버를 제거할 권한이 없습니다", code="FORBIDDEN"
        )

    # 마지막 OWNER 보호
    if target_role == MembershipRole.OWNER:
        if repo.count_owners(db, auth.org_id) <= 1:
            raise AppError(
                message="마지막 소유자는 제거할 수 없습니다", code="FORBIDDEN"
            )

    repo.delete_membership(db, auth.org_id, user_id)
    repo.release_member_seat(db, auth.org_id)


def change_member_role(
    db: Session, auth: AuthContext, user_id: _uuid.UUID, new_role: str
) -> Membership:
    """멤버 역할 변경.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    RBAC(OWNER 검증)은 router Depends에서 처리.
    """
    if auth.user_id == user_id:
        raise AppError(message="자신의 역할은 변경할 수 없습니다", code="VALIDATION_ERROR")

    # 역할 검증
    try:
        new_role_enum = MembershipRole(new_role)
    except ValueError:
        raise AppError(message="유효하지 않은 역할입니다", code="VALIDATION_ERROR")

    membership = repo.get_membership(db, user_id, auth.org_id)
    if not membership:
        raise AppError(message="해당 멤버를 찾을 수 없습니다", code="NOT_FOUND")

    current_role = MembershipRole(membership.role)
    if current_role == new_role_enum:
        raise AppError(message="이미 해당 역할입니다", code="VALIDATION_ERROR")

    # 마지막 OWNER 강등 방지
    if current_role == MembershipRole.OWNER:
        if repo.count_owners(db, auth.org_id) <= 1:
            raise AppError(
                message="마지막 소유자의 역할은 변경할 수 없습니다", code="FORBIDDEN"
            )

    membership.role = new_role_enum.value
    return membership


def get_first_membership_or_raise(
    db: Session, user_id: _uuid.UUID
) -> Membership:
    """유저의 첫 번째 멤버십 조회 + 미소속 시 에러.

    토큰 갱신 use_case에서 호출.
    """
    membership = repo.get_first_membership_or_none(db, user_id)
    if not membership:
        raise AppError(message="소속된 조직이 없습니다", code="FORBIDDEN")
    return membership


def check_not_member_by_email(
    db: Session, org_id: _uuid.UUID, user_id: _uuid.UUID
) -> None:
    """이미 조직 멤버이면 에러. 초대 생성 전 사전 검증."""
    existing = repo.get_membership(db, user_id, org_id)
    if existing:
        raise AppError(message="이미 조직에 소속된 멤버입니다", code="ALREADY_EXISTS")


def get_org_or_raise(db: Session, org_id: _uuid.UUID) -> Organization:
    """조직 조회 + 404 처리."""
    org = repo.get_org_by_id(db, org_id)
    if not org:
        raise AppError(message="조직을 찾을 수 없습니다", code="NOT_FOUND")
    return org


def add_member(
    db: Session, user_id: _uuid.UUID, org_id: _uuid.UUID, role: str
) -> Membership:
    """멤버십 추가 — 중복 검증 + 좌석 예약.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    내부에서 원자적 좌석 예약을 수행하므로 호출부에서 별도 한도 체크 불필요.
    """
    existing = repo.get_membership(db, user_id, org_id)
    if existing:
        raise AppError(message="이미 조직에 소속된 멤버입니다", code="ALREADY_EXISTS")

    if not repo.reserve_member_seat(db, org_id):
        raise AppError(
            message="멤버 수 한도를 초과했습니다. 플랜을 업그레이드해주세요.",
            code="MEMBER_LIMIT_EXCEEDED",
        )

    return repo.create_membership(db, user_id, org_id, role=role)


def check_credit_quota(db: Session, org_id: _uuid.UUID, feature: str) -> None:
    """AI 크레딧 잔량 읽기 전용 체크. 부족 시 QUOTA_EXCEEDED 발생."""
    cost = AI_CREDIT_COSTS.get(feature)
    if cost is None:
        return

    org = repo.get_org_by_id(db, org_id)
    if org is None:
        return

    if org.plan_credits_remaining + org.bonus_credits_remaining < cost:
        raise AppError(
            message="AI 크레딧이 부족합니다. 플랜을 업그레이드해주세요.",
            code="QUOTA_EXCEEDED",
        )


def check_storage_quota(db: Session, org_id: _uuid.UUID, additional_mb: int) -> None:
    """스토리지 한도 읽기 전용 체크. 초과 시 QUOTA_EXCEEDED 발생."""
    org = repo.get_org_by_id(db, org_id)
    if org is None:
        return

    if not org.allow_storage_overage and org.storage_mb_used + additional_mb > org.storage_mb_limit:
        raise AppError(
            message="스토리지 한도를 초과했습니다. 플랜을 업그레이드해주세요.",
            code="QUOTA_EXCEEDED",
        )


def set_profile_image(db: Session, auth: AuthContext, file: File) -> None:
    """프로필 이미지 설정 — 검증된 파일을 연결.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    org = get_org_or_raise(db, auth.org_id)
    org.set_profile_image(file)


def delete_profile_image(
    db: Session, auth: AuthContext, file_id: _uuid.UUID
) -> None:
    """프로필 이미지 제거 — 소프트 삭제는 FileHandler가 처리.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    org = get_org_or_raise(db, auth.org_id)
    org.remove_profile_image(file_id)
