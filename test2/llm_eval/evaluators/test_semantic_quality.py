"""LLM 매핑 품질 평가.

- 테스트라기보다 모델/프롬프트 회귀 성능 점검 용도
- 기본 실행에서는 제외되며, `--use-llm`로 수동 실행한다
"""

import json
import time
from pathlib import Path

import pytest

from app.infrastructure.excel_parser import extract_headers_and_rows
from app.infrastructure.llm_client import LLMModel
from app.modules.mapping.service import _determine_scope
from app.modules.ontology import service as ontology_service
from app.modules.ontology.service import MAPPING_SYSTEM_PROMPT

pytestmark = [pytest.mark.llm_eval, pytest.mark.costly]

LLM_DIR = Path(__file__).resolve().parents[1]
FIXTURES_DIR = LLM_DIR / "datasets" / "fixtures"
EXPECTED_DIR = LLM_DIR / "datasets" / "expected"

CSV_FILES = [
    "scope_full_bom.csv",
    "scope_part_list.csv",
    "scope_root_bom.csv",
]


def _load_expected(csv_name: str) -> dict:
    stem = csv_name.replace(".csv", "")
    path = EXPECTED_DIR / f"{stem}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def _check_single_run(mapping_result, expected: dict) -> list[str]:
    """단일 실행 결과를 기대값과 비교 후 실패 사유 목록을 반환한다."""
    failures: list[str] = []

    actual_props = {pm.source_column: pm.target_property for pm in mapping_result.property_mappings}
    for source_col, target_prop in expected["required_property_mappings"].items():
        actual_target = actual_props.get(source_col)
        if actual_target != target_prop:
            failures.append(
                f"property: {source_col} -> expected '{target_prop}', got '{actual_target}'"
            )

    if "required_relations" in expected:
        for exp_rel in expected["required_relations"]:
            rel_type = exp_rel["rel_type"]
            actual = next(
                (rm for rm in mapping_result.relation_mappings if rm.rel_type == rel_type),
                None,
            )
            if actual is None:
                failures.append(f"relation: {rel_type} missing")
                continue
            for prop, col in exp_rel.get("node_columns", {}).items():
                actual_col = actual.node_columns.get(prop)
                if actual_col != col:
                    failures.append(
                        f"relation({rel_type}).node_columns: {prop} -> expected '{col}', got '{actual_col}'"
                    )
            for prop, col in exp_rel.get("rel_columns", {}).items():
                actual_col = actual.rel_columns.get(prop)
                if actual_col != col:
                    failures.append(
                        f"relation({rel_type}).rel_columns: {prop} -> expected '{col}', got '{actual_col}'"
                    )
    else:
        actual_rel_types = {rm.rel_type for rm in mapping_result.relation_mappings}
        for rel_type in expected.get("required_relation_types", []):
            if rel_type not in actual_rel_types:
                failures.append(f"relation: {rel_type} missing")

    actual_scope = _determine_scope(mapping_result)
    expected_scope = expected["expected_scope"]
    if actual_scope.value != expected_scope:
        failures.append(f"scope: expected '{expected_scope}', got '{actual_scope.value}'")

    return failures


@pytest.mark.parametrize("csv_file", CSV_FILES)
def test_mapping_semantic_quality(
    csv_file: str,
    llm_runs: int,
    llm_model: LLMModel | None,
    report_collector: dict,
):
    csv_path = FIXTURES_DIR / csv_file
    content = csv_path.read_bytes()
    headers, sample_rows = extract_headers_and_rows(content, csv_file)
    expected = _load_expected(csv_file)

    details: list[dict] = []
    for i in range(llm_runs):
        start = time.perf_counter()
        try:
            mapping_result, llm_resp = ontology_service.generate_mapping(
                headers,
                sample_rows,
                model=llm_model,
            )
        except Exception as exc:
            elapsed = round(time.perf_counter() - start, 2)
            details.append(
                {
                    "run": i + 1,
                    "passed": False,
                    "elapsed_s": elapsed,
                    "failures": [f"error: {exc}"],
                    "raw_response": None,
                }
            )
            continue

        elapsed = round(time.perf_counter() - start, 2)
        failures = _check_single_run(mapping_result, expected)
        details.append(
            {
                "run": i + 1,
                "passed": len(failures) == 0,
                "elapsed_s": elapsed,
                "failures": failures,
                "raw_response": llm_resp.content,
            }
        )

        if not report_collector["meta"]:
            report_collector["meta"].update(
                model=llm_resp.model,
                system_prompt=MAPPING_SYSTEM_PROMPT,
                llm_runs=llm_runs,
            )

    pass_count = sum(1 for d in details if d["passed"])
    total = len(details)
    pass_rate = round(pass_count / total, 2) if total else 0

    elapsed_values = [d["elapsed_s"] for d in details]
    avg_elapsed = round(sum(elapsed_values) / len(elapsed_values), 2) if elapsed_values else 0
    report_collector["cases"].append(
        {
            "csv_file": csv_file,
            "runs": total,
            "passed": pass_count,
            "pass_rate": pass_rate,
            "avg_elapsed_s": avg_elapsed,
            "details": details,
        }
    )

    print(f"\\n{'=' * 60}")
    print(f"{csv_file} -- {pass_count}/{total} passed ({pass_rate * 100:.0f}%)")
    print(f"{'=' * 60}")

    for d in details:
        status = "PASS" if d["passed"] else "FAIL"
        print(f"  Run {d['run']}: {status} ({d['elapsed_s']}s)")
        for failure in d["failures"]:
            print(f"    - {failure}")

    assert pass_count == total, f"{csv_file}: {total - pass_count}/{total} runs failed"
