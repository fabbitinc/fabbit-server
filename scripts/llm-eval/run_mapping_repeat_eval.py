#!/usr/bin/env python3
"""LLM 매핑 호출 반복 평가 스크립트.

일반 단위 테스트와 분리하여, 실제 LLM 응답의 일관성과 기대 매핑 충족률을 반복 측정한다.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import statistics
import sys
import time
from dataclasses import dataclass
from datetime import datetime, UTC
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from app.infrastructure.excel_parser import extract_headers_and_rows
from app.modules.ontology import service as ontology_service
from app.modules.ontology.schemas import MappingResult


@dataclass
class ExpectRelationProperty:
    rel_type: str
    rel_property: str


@dataclass
class ExpectColumnTarget:
    label: str
    prop: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="LLM 매핑 반복 평가")
    parser.add_argument(
        "--file",
        default="sample/hierarchical_bom.csv",
        help="평가용 입력 파일 경로",
    )
    parser.add_argument("--sheet", default=None, help="엑셀 시트명(선택)")
    parser.add_argument("--rows", type=int, default=5, help="샘플 행 수")
    parser.add_argument("--runs", type=int, default=10, help="반복 호출 횟수")
    parser.add_argument(
        "--sleep-ms",
        type=int,
        default=0,
        help="호출 간 대기 시간(ms)",
    )
    parser.add_argument(
        "--expect-relation-property",
        action="append",
        default=[],
        metavar="REL:PROP",
        help="매 호출에서 기대하는 관계 속성. 예: SUPPLIED_BY:unit_cost",
    )
    parser.add_argument(
        "--expect-column-target",
        action="append",
        default=[],
        metavar="LABEL:PROP",
        help="매 호출에서 기대하는 노드 속성 타겟. 예: Supplier:company_name",
    )
    parser.add_argument(
        "--forbid-ext-pattern",
        action="append",
        default=[],
        metavar="REGEX",
        help="ext source/property에서 금지할 정규식",
    )
    parser.add_argument(
        "--report",
        default=None,
        help="리포트 JSON 출력 경로(미지정 시 자동 파일명)",
    )
    parser.add_argument(
        "--strict-exit",
        action="store_true",
        help="기대 조건 미충족이 1건이라도 있으면 exit code 1",
    )
    return parser.parse_args()


def parse_relation_property(raw: str) -> ExpectRelationProperty:
    rel_type, sep, rel_prop = raw.partition(":")
    if not sep or not rel_type or not rel_prop:
        raise ValueError(f"잘못된 --expect-relation-property 형식: {raw}")
    return ExpectRelationProperty(
        rel_type=rel_type.strip(), rel_property=rel_prop.strip()
    )


def parse_column_target(raw: str) -> ExpectColumnTarget:
    label, sep, prop = raw.partition(":")
    if not sep or not label or not prop:
        raise ValueError(f"잘못된 --expect-column-target 형식: {raw}")
    return ExpectColumnTarget(label=label.strip(), prop=prop.strip())


def mapping_fingerprint(mapping: MappingResult) -> str:
    payload = mapping.model_dump(mode="json")
    canonical = json.dumps(payload, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]


def has_relation_property(
    mapping: MappingResult, expected: ExpectRelationProperty
) -> bool:
    for rm in mapping.relation_mappings:
        if rm.rel_type != expected.rel_type:
            continue
        if expected.rel_property in rm.properties.values():
            return True
    return False


def has_column_target(mapping: MappingResult, expected: ExpectColumnTarget) -> bool:
    for cm in mapping.column_mappings:
        if cm.target_label == expected.label and cm.target_property == expected.prop:
            return True
    return False


def has_forbidden_ext(mapping: MappingResult, pattern: re.Pattern[str]) -> bool:
    for ep in mapping.extended_properties:
        if pattern.search(ep.source_column) or pattern.search(ep.property_name):
            return True
    return False


def auto_report_path() -> Path:
    now = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    return Path("reports/llm-eval") / f"mapping-repeat-{now}.json"


def main() -> int:
    args = parse_args()

    sample_path = Path(args.file)
    if not sample_path.exists():
        raise FileNotFoundError(f"샘플 파일이 없습니다: {sample_path}")

    raw_bytes = sample_path.read_bytes()
    headers, sample_rows = extract_headers_and_rows(
        raw_bytes,
        sample_path.name,
        sheet_name=args.sheet,
        max_rows=args.rows,
    )
    if not headers:
        raise ValueError("헤더를 추출할 수 없습니다")

    expected_rel_props = [
        parse_relation_property(raw) for raw in args.expect_relation_property
    ]
    expected_column_targets = [
        parse_column_target(raw) for raw in args.expect_column_target
    ]
    forbidden_patterns = [
        re.compile(raw, re.IGNORECASE) for raw in args.forbid_ext_pattern
    ]

    run_results: list[dict] = []
    elapsed_list: list[float] = []
    input_tokens: list[int] = []
    output_tokens: list[int] = []

    for run_idx in range(1, args.runs + 1):
        started = time.perf_counter()
        mapping, llm_resp = ontology_service.generate_mapping(headers, sample_rows)
        elapsed = time.perf_counter() - started

        elapsed_list.append(elapsed)
        input_tokens.append(llm_resp.input_tokens)
        output_tokens.append(llm_resp.output_tokens)

        rel_prop_missing = [
            f"{it.rel_type}:{it.rel_property}"
            for it in expected_rel_props
            if not has_relation_property(mapping, it)
        ]
        column_target_missing = [
            f"{it.label}:{it.prop}"
            for it in expected_column_targets
            if not has_column_target(mapping, it)
        ]
        forbidden_ext_hits = [
            pattern.pattern
            for pattern in forbidden_patterns
            if has_forbidden_ext(mapping, pattern)
        ]

        run_results.append(
            {
                "run": run_idx,
                "elapsed_sec": round(elapsed, 3),
                "model": llm_resp.model,
                "input_tokens": llm_resp.input_tokens,
                "output_tokens": llm_resp.output_tokens,
                "fingerprint": mapping_fingerprint(mapping),
                "counts": {
                    "column_mappings": len(mapping.column_mappings),
                    "relation_mappings": len(mapping.relation_mappings),
                    "extended_properties": len(mapping.extended_properties),
                },
                "missing_expected_relation_properties": rel_prop_missing,
                "missing_expected_column_targets": column_target_missing,
                "forbidden_ext_pattern_hits": forbidden_ext_hits,
                "mapping": mapping.model_dump(mode="json"),
            }
        )

        print(
            f"[{run_idx}/{args.runs}] {elapsed:.2f}s | fp={run_results[-1]['fingerprint']} "
            f"| rel={len(mapping.relation_mappings)} ext={len(mapping.extended_properties)}"
        )

        if args.sleep_ms > 0 and run_idx < args.runs:
            time.sleep(args.sleep_ms / 1000)

    fingerprints = [it["fingerprint"] for it in run_results]
    unique_fingerprints = sorted(set(fingerprints))
    rel_prop_miss_total = sum(
        len(it["missing_expected_relation_properties"]) for it in run_results
    )
    column_target_miss_total = sum(
        len(it["missing_expected_column_targets"]) for it in run_results
    )
    forbidden_ext_hit_total = sum(
        len(it["forbidden_ext_pattern_hits"]) for it in run_results
    )

    summary = {
        "sample": {
            "file": str(sample_path),
            "sheet": args.sheet,
            "rows": args.rows,
            "headers": headers,
        },
        "runs": args.runs,
        "latency": {
            "avg_sec": round(statistics.fmean(elapsed_list), 3),
            "min_sec": round(min(elapsed_list), 3),
            "max_sec": round(max(elapsed_list), 3),
        },
        "tokens": {
            "input_total": sum(input_tokens),
            "output_total": sum(output_tokens),
            "input_avg": round(statistics.fmean(input_tokens), 1),
            "output_avg": round(statistics.fmean(output_tokens), 1),
        },
        "consistency": {
            "unique_fingerprints": len(unique_fingerprints),
            "fingerprints": unique_fingerprints,
        },
        "expectation_violations": {
            "missing_relation_properties": rel_prop_miss_total,
            "missing_column_targets": column_target_miss_total,
            "forbidden_ext_hits": forbidden_ext_hit_total,
        },
    }

    report_path = Path(args.report) if args.report else auto_report_path()
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report = {"summary": summary, "runs": run_results}
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print("\n=== LLM 반복 평가 요약 ===")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    print(f"리포트 저장: {report_path}")

    has_violation = (
        rel_prop_miss_total > 0
        or column_target_miss_total > 0
        or forbidden_ext_hit_total > 0
    )
    if args.strict_exit and has_violation:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
