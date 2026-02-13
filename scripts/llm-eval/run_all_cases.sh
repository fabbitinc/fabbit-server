#!/usr/bin/env bash
set -euo pipefail

ROOT="scripts/llm-eval"
TS="$(date -u +%Y%m%dT%H%M%SZ)"

run_case() {
  local case_name="$1"
  shift
  local input_file="$ROOT/cases/$case_name/input.csv"
  local output_file="$ROOT/results/$case_name/mapping-repeat-$TS.json"

  echo "[run] $case_name"
  uv run python "$ROOT/run_mapping_repeat_eval.py" \
    --file "$input_file" \
    --runs 5 \
    --report "$output_file" \
    "$@"
}

run_case "hierarchical_bom" \
  --expect-relation-property "CONSISTS_OF:quantity" \
  --expect-column-target "Part:part_number" \
  --expect-column-target "Supplier:company_name" \
  --min-relation-mappings 2 \
  --max-extended-properties 0 \
  --forbid-ext-pattern "(단가|price|cost)"

run_case "messy_bom" \
  --expect-column-target "Part:part_number" \
  --expect-column-target "Part:name" \
  --expect-column-target "Part:lead_time_days" \
  --max-extended-properties 6 \
  --forbid-column-target "표면처리:Part:material" \
  --forbid-ext-pattern "_ext__ext_"

run_case "arduino_sheet1" \
  --expect-column-target "Part:part_number" \
  --expect-column-target "Supplier:company_name" \
  --min-relation-mappings 1 \
  --max-extended-properties 14 \
  --forbid-column-target "Value:Part:material" \
  --forbid-column-target "Critical:Part:is_phantom" \
  --forbid-ext-pattern "_ext__ext_"

echo "done: $ROOT/results/*/mapping-repeat-$TS.json"
