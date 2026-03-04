"""e2e 테스트 공용 fixture.

- 기본은 LLM 비활성
- `--use-llm` 옵션을 주면 LLM 호출 API 케이스까지 실행
"""

import json
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app

BASE_DIR = Path(__file__).resolve().parents[1]
FIXTURES_DIR = BASE_DIR / "shared" / "fixtures"
E2E_DIR = BASE_DIR / "e2e"

_covered_endpoints: set[tuple[str, str]] = set()


@app.middleware("http")
async def _record_endpoint_coverage(request, call_next):
    """테스트 중 호출된 라우트 패턴을 기록."""
    response = await call_next(request)
    route = request.scope.get("route")
    if route and hasattr(route, "path"):
        _covered_endpoints.add((request.method, route.path))
    return response


@pytest.fixture(scope="session")
def client():
    """FastAPI TestClient (세션 공유)."""
    with TestClient(app) as c:
        yield c


@pytest.fixture(scope="session")
def unique_suffix():
    """테스트 세션 고유 suffix."""
    return str(int(time.time()))


@pytest.fixture(scope="session")
def fixtures_dir():
    """공용 fixture 디렉터리 경로."""
    return FIXTURES_DIR


@pytest.fixture(scope="session")
def mapping_fixture():
    """고정 매핑 fixture (LLM 우회용)."""
    path = FIXTURES_DIR / "hierarchical_bom_mapping.json"
    return json.loads(path.read_text(encoding="utf-8"))


def _should_skip_uncovered_report(request) -> bool:
    """수집 전용 실행/비실행 상황에서는 파일 생성을 건너뛴다."""
    if request.config.option.collectonly:
        return True
    return False


@pytest.fixture(scope="session", autouse=True)
def _report_uncovered_endpoints(request, client):
    """세션 종료 시 미테스트 엔드포인트를 출력한다."""
    if _should_skip_uncovered_report(request):
        yield
        return

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
    if not uncovered:
        return

    output = E2E_DIR / "uncovered_endpoints.txt"
    with open(output, "w", encoding="utf-8") as file:
        file.write(f"# 미테스트 API 엔드포인트 ({len(uncovered)}/{len(all_endpoints)})\n\n")
        for method, path in uncovered:
            file.write(f"{method:6s} {path}\n")

    print(f"\n[커버리지] 미테스트 엔드포인트: {len(uncovered)}/{len(all_endpoints)} -> {output}")
