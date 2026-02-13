# LLM 반복 평가 스크립트

이 디렉터리는 일반 단위/API 테스트와 분리된 LLM 품질 점검용 스크립트를 관리합니다.

## 목적

- 동일 입력에 대한 LLM 매핑 응답의 일관성(드리프트) 확인
- 특정 기대 매핑의 충족률 확인
- ext 누락/오매핑 패턴을 반복 실행으로 관찰

## 폴더 구조

- `cases/hierarchical_bom/input.csv`
- `cases/messy_bom/input.csv`
- `cases/arduino_sheet1/input.csv`
- `results/<case>/` (실행 결과 저장)

## 실행 예시

```bash
uv run python scripts/llm-eval/run_mapping_repeat_eval.py \
  --file scripts/llm-eval/cases/hierarchical_bom/input.csv \
  --runs 5 \
  --report scripts/llm-eval/results/hierarchical_bom/result.json
```

3개 케이스를 각 5회씩 실행:

```bash
bash scripts/llm-eval/run_all_cases.sh
```

`run_all_cases.sh`는 케이스별 기대/금지 규칙을 함께 적용합니다.

## 주요 옵션

- `--runs`: 반복 호출 횟수
- `--rows`: 샘플 행 수(LLM 입력)
- `--expect-relation-property REL:PROP`: 기대 관계 속성
- `--expect-column-target LABEL:PROP`: 기대 노드 속성
- `--forbid-ext-pattern REGEX`: ext에서 금지할 패턴
- `--forbid-column-target SRC:LABEL:PROP`: 금지할 컬럼→타겟 매핑
- `--max-extended-properties`: 허용 가능한 ext 최대 개수
- `--min-relation-mappings`: 요구되는 relation 최소 개수
- `--report`: 리포트 저장 경로 지정(기본 `reports/llm-eval/*.json`)

## 비고

- 실제 OpenAI API를 호출하므로 비용/속도를 고려해 주기 배치(nightly, 릴리즈 전)로 운영하는 것을 권장합니다.
