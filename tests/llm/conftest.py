import json
from datetime import datetime
from pathlib import Path

import pytest

from app.infrastructure.llm_client import LLMModel

REPORT_DIR = Path(__file__).parent / "reports"


# ── 세션 공유 리포트 데이터 ──
_report_cases: list[dict] = []
_report_meta: dict = {}


def pytest_addoption(parser):
    parser.addoption(
        "--llm-runs",
        type=int,
        default=5,
        help="LLM 매핑 품질 테스트 반복 횟수 (기본값: 5)",
    )
    model_names = [m.name for m in LLMModel]
    parser.addoption(
        "--llm-model",
        type=str,
        default=None,
        choices=model_names,
        help=f"LLM 모델 지정: {', '.join(model_names)} (미지정 시 DEFAULT_MODEL 사용)",
    )


@pytest.fixture(scope="session")
def llm_runs(request):
    """LLM 테스트 반복 횟수."""
    return request.config.getoption("--llm-runs")


@pytest.fixture(scope="session")
def llm_model(request) -> LLMModel | None:
    """LLM 모델 (None이면 generate_mapping 기본값 사용)."""
    name = request.config.getoption("--llm-model")
    if name is None:
        return None
    return LLMModel[name]


@pytest.fixture(autouse=True)
def _require_llm_flag(use_llm):
    """--use-llm 플래그 없으면 스킵."""
    if not use_llm:
        pytest.skip("--use-llm 플래그 필요")


@pytest.fixture(scope="session")
def report_collector():
    """테스트 케이스별 결과를 수집하는 컬렉터."""
    return {"meta": _report_meta, "cases": _report_cases}


def pytest_sessionfinish(session, exitstatus):
    """세션 종료 시 리포트 JSON 파일 생성."""
    if not _report_cases:
        return

    total_runs = sum(c["runs"] for c in _report_cases)
    total_passed = sum(c["passed"] for c in _report_cases)

    report = {
        "timestamp": datetime.now().isoformat(timespec="seconds"),
        "model": _report_meta.get("model", "unknown"),
        "system_prompt": _report_meta.get("system_prompt", ""),
        "llm_runs_per_case": _report_meta.get("llm_runs", 0),
        "summary": {
            "total_cases": len(_report_cases),
            "total_runs": total_runs,
            "total_passed": total_passed,
            "pass_rate": round(total_passed / total_runs, 2) if total_runs else 0,
        },
        "cases": _report_cases,
    }

    REPORT_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_path = REPORT_DIR / f"mapping_quality_{timestamp}.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2))

    # 터미널에 경로 출력
    reporter = session.config.pluginmanager.get_plugin("terminalreporter")
    if reporter:
        reporter.write_line(f"\nReport saved: {report_path}")
