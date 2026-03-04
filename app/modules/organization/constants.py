"""조직 관련 상수 및 slug 검증."""

import re
from dataclasses import dataclass
from enum import Enum


class MembershipRole(str, Enum):
    """멤버십 역할."""

    MEMBER = "MEMBER"  # 일반 멤버
    ADMIN = "ADMIN"  # 관리자
    OWNER = "OWNER"  # 소유자


# 역할 계층 레벨 — 숫자가 높을수록 상위 권한
ROLE_LEVEL: dict[MembershipRole, int] = {
    MembershipRole.MEMBER: 0,
    MembershipRole.ADMIN: 1,
    MembershipRole.OWNER: 2,
}


def can_manage_role(actor_role: MembershipRole, target_role: MembershipRole) -> bool:
    """actor가 target 역할의 멤버를 관리(초대/제거)할 수 있는지 판단.

    - OWNER: 모든 역할 관리 가능 (같은 레벨 O)
    - ADMIN: MEMBER만 관리 가능 (같은 레벨 X)
    - MEMBER: 관리 불가
    """
    actor_level = ROLE_LEVEL.get(actor_role, 0)
    target_level = ROLE_LEVEL.get(target_role, 0)
    if actor_role == MembershipRole.OWNER:
        return True
    return actor_level > target_level


#   - 5명: 설계팀만 쓰는 최소 단위. "일단 써보자"의 진입점
#   - 20명: 설계 + 구매 + 생산 핵심 인력. 제안하신 10명은 제조업에서 약간 빡빡함 — 설계 5 + 구매 3 + 생산 2면 바로 찬다
#   - 50명: 30명도 괜찮지만 50명이면 "넉넉하다" 느낌을 줘서 업셀 시점을 늦출 수 있음. 대신 Business 가격을 그만큼 높게 잡으면 됨
#   - 무제한: Enterprise는 어차피 견적이니까 숫자 의미 없음

#   ┌────────────────┬───────────────────────────────────┬───────────────────────────────┐
#   │      기능      │        왜 차등이 타당한가         │             제한              │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ 인원           │ 서버 비용 비례                    │ 5 / 20 / 50 / 무제한          │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ 스토리지       │ 저장 비용 비례                    │ 2 / 100 / 500 / 2,000 GB      │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ AI 크레딧      │ 추론 비용 비례                    │ 100 / 3,000 / 10,000 / 50,000 │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ SSO/SAML       │ 중견 이상만 필요, 구현 비용 있음  │ Enterprise만                  │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ 감사 로그 보관 │ 저장 비용 + 컴플라이언스          │ 30일 / 1년 / 무제한           │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ API 접근       │ 연동 자동화는 규모 있는 조직 니즈 │ Business+                     │
#   ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
#   │ 전담 지원      │ 인건비                            │ Enterprise만                  │
#   └────────────────┴───────────────────────────────────┴───────────────────────────────┘


#   ┌──────┬──────────────────┬────────────────────────────────────────────────────────────┐
#   │ 단계 │       시점       │                            대응                            │
#   ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
#   │ 지금 │ 출시 전          │ 무시. SaaS에 집중                                          │
#   ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
#   │ 초기 │ ~100개사         │ "Enterprise 플랜에서 검토 중"으로 응대                     │
#   ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
#   │ 중기 │ 100개사+         │ VPC 배포 (고객 AWS 계정에 설치) — 온프레미스보다 훨씬 쉬움 │
#   ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
#   │ 후기 │ 대기업 계약 확보 │ 진짜 온프레미스 or 하이브리드                              │
#   └──────┴──────────────────┴────────────────────────────────────────────────────────────┘


class PlanType(str, Enum):
    STARTER = "STARTER"
    TEAM = "TEAM"
    BUSINESS = "BUSINESS"
    ENTERPRISE = "ENTERPRISE"


_GB_TO_BYTES = 1_000_000_000


@dataclass(frozen=True)
class PlanLimits:
    max_members: int  # 최대 멤버 수 (-1 = 무제한)
    storage_gb: int  # 월 스토리지 GB
    ai_credits: int  # 월 AI 크레딧
    price_monthly: int  # 월 요금 (원, -1 = 견적 기반)
    display_name: str  # 표시명
    description: str  # 설명

    @property
    def storage_bytes(self) -> int:
        """스토리지 한도를 bytes로 변환."""
        return self.storage_gb * _GB_TO_BYTES


# AI 크레딧 소비 단가 (기능별)
AI_CREDIT_COSTS: dict[str, int] = {
    "chat": 1,  # AI 채팅 1회
    "bom_analysis": 5,  # BOM 분석 1건
    "drawing_parse": 10,  # 도면 분석 1건
}


PLAN_LIMITS: dict[PlanType, PlanLimits] = {
    PlanType.STARTER: PlanLimits(
        max_members=5,
        storage_gb=2,
        ai_credits=100,
        price_monthly=0,
        display_name="Starter",
        description="소규모 팀이 부담 없이 시작할 수 있는 상시 무료 플랜",
    ),
    PlanType.TEAM: PlanLimits(
        max_members=20,
        storage_gb=100,
        ai_credits=3_000,
        price_monthly=249_000,
        display_name="Team",
        description="실무 운영에 맞춘 중소 제조팀 기본 플랜",
    ),
    PlanType.BUSINESS: PlanLimits(
        max_members=50,
        storage_gb=500,
        ai_credits=10_000,
        price_monthly=599_000,
        display_name="Business",
        description="부서 간 협업과 대량 처리가 필요한 조직용 플랜",
    ),
    PlanType.ENTERPRISE: PlanLimits(
        max_members=-1,
        storage_gb=2_000,
        ai_credits=50_000,
        price_monthly=-1,
        display_name="Enterprise",
        description="전사 도입과 맞춤 운영이 필요한 대규모 조직용 플랜",
    ),
}

RESERVED_SLUGS: frozenset[str] = frozenset(
    {
        # 인프라 / 시스템
        "www",
        "www1",
        "www2",
        "web",
        "site",
        "api",
        "app",
        "cdn",
        "static",
        "assets",
        "media",
        "mail",
        "smtp",
        "imap",
        "pop",
        "mx",
        "ftp",
        "sftp",
        "ssh",
        "ns1",
        "ns2",
        "ns3",
        "ns4",
        "dns",
        "vpn",
        "proxy",
        "gateway",
        # 환경
        "dev",
        "staging",
        "test",
        "qa",
        "uat",
        "sandbox",
        "prod",
        "production",
        "preview",
        "canary",
        "local",
        "localhost",
        # 서비스 / 내부 도구
        "admin",
        "dashboard",
        "console",
        "panel",
        "auth",
        "login",
        "signup",
        "register",
        "sso",
        "oauth",
        "billing",
        "payment",
        "checkout",
        "help",
        "support",
        "docs",
        "wiki",
        "faq",
        "blog",
        "news",
        "press",
        "status",
        "health",
        "monitor",
        "metrics",
        "grafana",
        # 브랜드 보호
        "fabbit",
        "fabbitinc",
        "fabbitapp",
        # 악용 방지
        "abuse",
        "spam",
        "phishing",
        "security",
        "postmaster",
        "webmaster",
        "hostmaster",
        "noreply",
        "no-reply",
        "mailer-daemon",
        "root",
        "sysadmin",
        "administrator",
        # 기타
        "internal",
        "intranet",
        "extranet",
        "download",
        "downloads",
        "update",
        "updates",
    }
)

# slug 포맷: 소문자 영숫자 + 하이픈, 3~50자, 하이픈으로 시작/끝 불가
_SLUG_PATTERN = re.compile(r"^[a-z0-9](?:[a-z0-9-]{1,48}[a-z0-9])?$")


def validate_slug_format(slug: str) -> str | None:
    """slug 포맷 검증. 문제가 있으면 에러 메시지 반환, 없으면 None."""
    if len(slug) < 3:
        return "슬러그는 최소 3자 이상이어야 합니다"
    if len(slug) > 50:
        return "슬러그는 최대 50자까지 가능합니다"
    if not _SLUG_PATTERN.match(slug):
        return "소문자 영문, 숫자, 하이픈(-)만 사용 가능하며, 하이픈으로 시작/끝할 수 없습니다"
    if slug in RESERVED_SLUGS:
        return "사용할 수 없는 워크스페이스 주소입니다"
    return None


# 종량제 추가 구매 단가
OVERAGE_PRICING: dict[str, int] = {
    "storage_per_gb": 500,  # ₩500/GB/월
    "ai_credit_per_unit": 50,  # ₩50/크레딧 (1,000개 = ₩50,000)
}
