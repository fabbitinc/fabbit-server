"""LLM 평가 테스트 공용 설정.

주의: 이 영역은 회귀 성능/품질 검증 용도이며, 기본 테스트 루틴에서 제외된다.
"""

import json
from datetime import datetime
from pathlib import Path

import pytest

from app.infrastructure.llm_client import LLMModel

LLM_DIR = Path(__file__).parent
REPORT_DIR = LLM_DIR / "reports"

_report_cases: list[dict] = []
_report_meta: dict = {}


def pytest_addoption(parser):
    parser.addoption(
        "--llm-runs",
        type=int,
        default=3,
        help="LLM 평가 반복 횟수 (기본값: 3)",
    )
    model_names = [m.name for m in LLMModel]
    parser.addoption(
        "--llm-model",
        type=str,
        default=None,
        choices=model_names,
        help=f"LLM 모델 지정: {', '.join(model_names)}",
    )


@pytest.fixture(scope="session")
def llm_runs(request):
    return request.config.getoption("--llm-runs")


@pytest.fixture(scope="session")
def llm_model(request) -> LLMModel | None:
    name = request.config.getoption("--llm-model")
    if name is None:
        return None
    return LLMModel[name]


@pytest.fixture(autouse=True)
def _require_llm_flag(use_llm):
    if not use_llm:
        pytest.skip("--use-llm 플래그 필요")


@pytest.fixture(scope="session")
def report_collector():
    return {"meta": _report_meta, "cases": _report_cases}


def pytest_sessionfinish(session, exitstatus):
    if not _report_cases:
        return

    total_runs = sum(c["runs"] for c in _report_cases)
    total_passed = sum(c["passed"] for c in _report_cases)
    all_elapsed = [d["elapsed_s"] for c in _report_cases for d in c.get("details", [])]
    total_elapsed = round(sum(all_elapsed), 2)
    avg_elapsed = round(total_elapsed / len(all_elapsed), 2) if all_elapsed else 0

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
            "total_elapsed_s": total_elapsed,
            "avg_elapsed_s": avg_elapsed,
        },
        "cases": _report_cases,
    }

    REPORT_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_path = REPORT_DIR / f"mapping_quality_{timestamp}.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    reporter = session.config.pluginmanager.get_plugin("terminalreporter")
    if reporter:
        reporter.write_line(f"\\nReport saved: {report_path}")
