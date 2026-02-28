"""조직 관련 상수 및 slug 검증."""

import re
from dataclasses import dataclass
from enum import Enum


class MembershipRole(str, Enum):
    """멤버십 역할."""

    MEMBER = "MEMBER"   # 일반 멤버
    ADMIN = "ADMIN"     # 관리자


class PlanType(str, Enum):
    STARTER = "STARTER"
    TEAM = "TEAM"
    ENTERPRISE = "ENTERPRISE"


@dataclass(frozen=True)
class PlanLimits:
    storage_gb: int           # 월 스토리지 GB
    max_bom: int              # 월 BOM 분석 건수
    max_drawing_parses: int   # 월 도면 분석 건수
    max_chats: int            # 월 채팅 횟수
    price_monthly: int        # 월 요금 (원)
    display_name: str         # 표시명
    description: str          # 설명


PLAN_LIMITS: dict[PlanType, PlanLimits] = {
    PlanType.STARTER: PlanLimits(
        storage_gb=2,
        max_bom=50,
        max_drawing_parses=10,
        max_chats=200,
        price_monthly=0,
        display_name="Starter",
        description="소규모 팀이 부담 없이 시작할 수 있는 상시 무료 플랜",
    ),
    PlanType.TEAM: PlanLimits(
        storage_gb=100,
        max_bom=3_000,
        max_drawing_parses=300,
        max_chats=3_000,
        price_monthly=249_000,
        display_name="Team",
        description="5~10인 팀의 실무 운영에 맞춘 기본 플랜",
    ),
    PlanType.ENTERPRISE: PlanLimits(
        storage_gb=1_000,
        max_bom=30_000,
        max_drawing_parses=3_000,
        max_chats=30_000,
        price_monthly=999_000,
        display_name="Enterprise",
        description="대량 처리와 안정적 운영이 필요한 조직용 플랜",
    ),
}

RESERVED_SLUGS: frozenset[str] = frozenset({
    # 인프라 / 시스템
    "www", "www1", "www2", "web", "site",
    "api", "app", "cdn", "static", "assets", "media",
    "mail", "smtp", "imap", "pop", "mx",
    "ftp", "sftp", "ssh",
    "ns1", "ns2", "ns3", "ns4", "dns",
    "vpn", "proxy", "gateway",
    # 환경
    "dev", "staging", "test", "qa", "uat", "sandbox",
    "prod", "production", "preview", "canary",
    "local", "localhost",
    # 서비스 / 내부 도구
    "admin", "dashboard", "console", "panel",
    "auth", "login", "signup", "register", "sso", "oauth",
    "billing", "payment", "checkout",
    "help", "support", "docs", "wiki", "faq",
    "blog", "news", "press",
    "status", "health", "monitor", "metrics", "grafana",
    # 브랜드 보호
    "fabbit", "fabbitinc", "fabbitapp",
    # 악용 방지
    "abuse", "spam", "phishing", "security",
    "postmaster", "webmaster", "hostmaster",
    "noreply", "no-reply", "mailer-daemon",
    "root", "sysadmin", "administrator",
    # 기타
    "internal", "intranet", "extranet",
    "download", "downloads", "update", "updates",
})

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
