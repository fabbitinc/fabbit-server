"""회원가입 관련 상수 및 slug 검증."""

import re
from dataclasses import dataclass
from enum import Enum


class PlanType(str, Enum):
    FREE = "FREE"
    PRO = "PRO"
    ELITE = "ELITE"


@dataclass(frozen=True)
class PlanLimits:
    max_members: int          # 최대 사용자 수 (-1 = 무제한)
    storage_gb: int           # 스토리지 GB
    max_bom: int              # BOM 건수 (-1 = 무제한)
    max_drawing_parses: int   # 월 도면 AI 파싱 건수 (-1 = 무제한)
    price_monthly: int        # 월 요금 (원)
    display_name: str         # 표시명
    description: str          # 설명


PLAN_LIMITS: dict[PlanType, PlanLimits] = {
    PlanType.FREE: PlanLimits(
        max_members=3,
        storage_gb=1,
        max_bom=100,
        max_drawing_parses=0,
        price_monthly=0,
        display_name="Free",
        description="소규모 팀의 첫 시작에 적합합니다",
    ),
    PlanType.PRO: PlanLimits(
        max_members=20,
        storage_gb=50,
        max_bom=-1,
        max_drawing_parses=500,
        price_monthly=49_000,
        display_name="Pro",
        description="성장하는 팀을 위한 전문 PLM",
    ),
    PlanType.ELITE: PlanLimits(
        max_members=-1,
        storage_gb=500,
        max_bom=-1,
        max_drawing_parses=-1,
        price_monthly=149_000,
        display_name="Elite",
        description="대규모 조직을 위한 엔터프라이즈급",
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
