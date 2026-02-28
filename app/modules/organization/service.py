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

from app.core.exceptions import AppError
from app.modules.organization import repository as repo
from app.modules.organization.constants import (
    RESERVED_SLUGS,
    MembershipRole,
    PlanType,
    validate_slug_format,
)
from app.modules.organization.models import Membership, Organization
from app.modules.organization.provisioning import provision_tenant
from app.modules.organization.schemas import (
    CreateOrganizationRequest,
    OrganizationResponse,
)


def _slugify(name: str) -> str:
    """조직명을 URL-safe slug로 변환."""
    # 유니코드 정규화 후 ASCII 변환
    name = unicodedata.normalize("NFKD", name)
    # 한글 등 non-ASCII는 유지
    slug = re.sub(r"[^\w\s-]", "", name).strip().lower()
    slug = re.sub(r"[-\s]+", "-", slug)
    return slug[:50]


def complete_onboarding(db: Session, auth: AuthContext) -> OrganizationResponse:
    """조직 온보딩 완료 처리.

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    org = repo.get_org_by_id(db, auth.org_id)
    if not org:
        raise AppError(message="조직을 찾을 수 없습니다", code="NOT_FOUND")
    if org.onboarded_at:
        raise AppError(message="이미 온보딩이 완료된 조직입니다", code="ALREADY_EXISTS")

    org = repo.complete_onboarding(db, auth.org_id)
    return OrganizationResponse.model_validate(org)


def create_organization(
    db: Session, user_id: _uuid.UUID, req: CreateOrganizationRequest
) -> Organization:
    """조직 생성 + 멤버십(ADMIN) + 프로비저닝 (토큰 발급 제외).

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

    # 조직 생성
    org = repo.create_organization(
        db,
        slug,
        req.org_name,
        user_id,
        industry=req.industry,
        team_size=req.team_size,
        plan_type=plan.value,
    )

    # 멤버십 (ADMIN)
    repo.create_membership(db, user_id, org.id, role=MembershipRole.ADMIN)

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
    RBAC(ADMIN 검증)은 router Depends(require_admin)에서 처리.
    """
    if auth.user_id == user_id:
        raise AppError(message="자신을 제거할 수 없습니다", code="VALIDATION_ERROR")

    # 소유자 보호
    org = repo.get_org_by_id(db, auth.org_id)
    if org and org.owner_id == user_id:
        raise AppError(message="조직 소유자는 제거할 수 없습니다", code="FORBIDDEN")

    membership = repo.get_membership(db, user_id, auth.org_id)
    if not membership:
        raise AppError(message="해당 멤버를 찾을 수 없습니다", code="NOT_FOUND")

    repo.delete_membership(db, auth.org_id, user_id)


def add_member(
    db: Session, user_id: _uuid.UUID, org_id: _uuid.UUID, role: str
) -> Membership:
    """멤버십 추가 (초대 수락 use_case용).

    @transactional 없음 — use_case에서 트랜잭션 관리.
    """
    return repo.create_membership(db, user_id, org_id, role=role)
