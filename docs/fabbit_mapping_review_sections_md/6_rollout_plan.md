# 6. 단계적 롤아웃 계획(리스크 최소화)

## Step 0 — 계측부터(기능 변경 없음)
- 지표:
  - ext_rate
  - missing_relation_slot_rate
  - unit_cost_miss_rate
  - user_fix_rate(사용자 편집/수정률)

## Step 1 — 관계 슬롯 템플릿화(구조 리스크 제거)
- ontology 기준으로 관계 타입별 슬롯을 항상 생성
- LLM 출력에서 누락되어도 UI 편집으로 복구 가능

## Step 2 — validate에 suggestions만 추가(자동 적용 없음)
- 후보 + blockers + 근거 + patch를 제공
- 적용은 사용자 클릭으로만

## Step 3 — patch 적용 API 추가
- `POST /mappings/{job_id}/apply` with JSON Patch
- 적용 후 즉시 validate 재실행(즉시 피드백)

## Step 4 — 추천 품질 개선(LLM 없이)
- aliases(온톨로지 메타) 누적
- 과거 사용자 확정 데이터를 통한 통계 기반 prior(룩업) 추가

## Step 5 — LLM은 낮은 커버리지 구간에만 제한
- 후보 점수가 낮고 의미가 복잡한 경우에만 LLM 질의
- LLM은 “정답”이 아니라 “후보/근거/주의사항”만 생성
