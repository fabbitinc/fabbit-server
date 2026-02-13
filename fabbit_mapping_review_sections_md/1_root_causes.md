# 1. ext로 빠지는 근본 원인 분해

아래 원인들은 “LLM 성능” 문제가 아니라 **현재 매핑 구조/제약 조건에서 구조적으로 발생**할 수 있는 문제입니다.

## A. 표현력/출력 스키마의 비대칭(노드 속성은 쉽고, 관계 속성은 어렵다)
- 현재 매핑의 1차 경로는 `column_mappings: source_column -> (target_label, target_property)`입니다.
- 반면, 관계 속성(예: `SUPPLIED_BY.unit_cost`)은 다음 조건이 동시에 만족해야 합니다.
  - 관계 매핑 슬롯이 존재해야 함(`relation_mappings.SUPPLIED_BY`)
  - from/to endpoint 키 컬럼이 유효해야 함(Part/Supplier 키)
  - 관계 속성 타입/required 제약까지 맞아야 함
- LLM 입장에서는 “틀릴 확률이 높은 구조(관계 속성)”보다 “안전한 ext”로 보내는 편이 더 보수적입니다.

## B. 한국어 컬럼명 ↔ 영어 온톨로지 속성명 매칭 약함(동의어/별칭 메타데이터 부재)
- `단가(원)` ↔ `unit_cost`는 언어가 다르고, “단가” 자체가 다의적입니다.
- 온톨로지가 `unit_cost`에 대해 `단가/매입단가/공급단가/KRW per unit` 같은 **별칭(aliases)**을 제공하지 않으면, 추천/매칭이 약해집니다.
- 해결은 “가격이면 무조건 unit_cost” 하드코딩이 아니라, 온톨로지 메타데이터에 **altLabel(동의어/대체 레이블)**을 추가하는 방식이 더 일반적입니다.
  - SKOS의 `skos:altLabel` 패턴 참고: https://www.w3.org/TR/skos-reference/

## C. Phase 1 정책과 ‘LLM 출력 누락’ 결합 시 사용자 복구 불가능
- 정책: 관계 생성/삭제 금지, `rel_type` 변경 금지.
- LLM이 `SUPPLIED_BY` 관계 매핑 자체를 누락하면, 사용자는 `unit_cost`를 붙일 “대상 관계”가 없어집니다.
- 즉 핵심 문제는 `unit_cost` 매핑 실패가 아니라, **관계 슬롯 존재 자체가 LLM 출력에 종속**되는 구조입니다.

## D. 데이터 컨텍스트 부족 시 `SUPPLIED_BY`로 붙이는 게 논리적으로 위험
- `단가(원)`은 공급단가/판매단가/내부원가 등으로 해석될 수 있습니다.
- 공급사 식별 컬럼(공급처, vendor_code 등)이 없으면 `SUPPLIED_BY`로 연결하는 것은 모델 오류 가능성이 큽니다.
- LLM이 리스크를 감지하면 ext로 보내는 게 합리적인 선택일 수 있습니다.

## E. validate가 “제약 검증”만 하고 “복구 경로(후보/필요조건)”를 제공하지 않는다
- 현재 validate는 존재/타입/required 등을 체크하지만,
  - “이 ext 컬럼은 온톨로지 어디에 붙일 후보가 있고”
  - “붙이려면 어떤 endpoint가 추가로 필요하다”
  같은 **복구 동선**(candidate + blockers + patch)이 없습니다.
- 결과적으로 ext가 남아도 사용자 편집이 어렵고, confirm 이후에는 더 복구가 어려워집니다.
