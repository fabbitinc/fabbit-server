"""pytest 공용 fixture."""

import json
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app

FIXTURES_DIR = Path(__file__).resolve().parent / "fixtures"
INTEGRATION_DIR = Path(__file__).resolve().parent / "integration"

# ── 엔드포인트 커버리지 추적 ──

_covered_endpoints: set[tuple[str, str]] = set()


@app.middleware("http")
async def _record_endpoint_coverage(request, call_next):
    """테스트 중 호출된 라우트 패턴을 기록."""
    response = await call_next(request)
    route = request.scope.get("route")
    if route and hasattr(route, "path"):
        _covered_endpoints.add((request.method, route.path))
    return response


def pytest_addoption(parser):
    parser.addoption(
        "--use-llm",
        action="store_true",
        default=False,
        help="LLM 호출이 필요한 테스트 활성화 (매핑 미리보기, AI 질의)",
    )


@pytest.fixture(scope="session")
def use_llm(request):
    """--use-llm 플래그 여부."""
    return request.config.getoption("--use-llm")


@pytest.fixture(scope="session")
def client():
    """FastAPI TestClient (세션 단위 공유)."""
    with TestClient(app) as c:
        yield c


@pytest.fixture(scope="session")
def unique_suffix():
    """테스트 세션마다 고유한 타임스탬프 suffix (데이터 격리)."""
    return str(int(time.time()))


@pytest.fixture(scope="session")
def fixtures_dir():
    """tests/fixtures/ 디렉토리 경로."""
    return FIXTURES_DIR


@pytest.fixture(scope="session")
def mapping_fixture():
    """고정 매핑 fixture (LLM 우회용)."""
    path = FIXTURES_DIR / "hierarchical_bom_mapping.json"
    return json.loads(path.read_text(encoding="utf-8"))


# ── 세션 종료 시 미커버 엔드포인트 출력 ──


@pytest.fixture(scope="session", autouse=True)
def _report_uncovered_endpoints(client):
    """테스트 시작 시 openapi.json 갱신, 종료 후 미테스트 엔드포인트를 파일로 출력."""
    # TestClient로 최신 OpenAPI 스펙 가져오기
    resp = client.get("/openapi.json")
    if resp.status_code != 200:
        yield
        return
    spec = resp.json()

    yield

    all_endpoints: set[tuple[str, str]] = set()
    for path, methods in spec.get("paths", {}).items():
        for method in methods:
            if method.upper() in ("GET", "POST", "PUT", "PATCH", "DELETE"):
                all_endpoints.add((method.upper(), path))

    uncovered = sorted(all_endpoints - _covered_endpoints)

    output = INTEGRATION_DIR / "uncovered_endpoints.txt"
    with open(output, "w", encoding="utf-8") as f:
        f.write(f"# 미테스트 API 엔드포인트 ({len(uncovered)}/{len(all_endpoints)})\n\n")
        for method, path in uncovered:
            f.write(f"{method:6s} {path}\n")
    if uncovered:
        print(f"\n[커버리지] 미테스트 엔드포인트: {len(uncovered)}/{len(all_endpoints)} → {output}")
