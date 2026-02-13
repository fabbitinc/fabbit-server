# LLM 반복 평가 스크립트

이 디렉터리는 일반 단위/API 테스트와 분리된 LLM 품질 점검용 스크립트를 관리합니다.

## 목적

- 동일 입력에 대한 LLM 매핑 응답의 일관성(드리프트) 확인
- 특정 기대 매핑의 충족률 확인
- ext 누락/오매핑 패턴을 반복 실행으로 관찰

## 실행 예시

```bash
uv run python scripts/llm-eval/run_mapping_repeat_eval.py \
  --file sample/hierarchical_bom.csv \
  --runs 20 \
  --expect-relation-property SUPPLIED_BY:unit_cost \
  --expect-column-target Supplier:company_name \
  --forbid-ext-pattern "(단가|price|cost)" \
  --strict-exit
```

## 주요 옵션

- `--runs`: 반복 호출 횟수
- `--rows`: 샘플 행 수(LLM 입력)
- `--expect-relation-property REL:PROP`: 기대 관계 속성
- `--expect-column-target LABEL:PROP`: 기대 노드 속성
- `--forbid-ext-pattern REGEX`: ext에서 금지할 패턴
- `--strict-exit`: 기대 위반이 있으면 종료 코드 1 반환
- `--report`: 리포트 저장 경로 지정(기본 `reports/llm-eval/*.json`)

## 비고

- 실제 OpenAI API를 호출하므로 비용/속도를 고려해 주기 배치(nightly, 릴리즈 전)로 운영하는 것을 권장합니다.
