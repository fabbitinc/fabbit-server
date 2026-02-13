# 2. 하드코딩 없이(ontology-driven) 후보 제안/검증 설계

목표는 서버가 정답을 강제하지 않고, **온톨로지 전체 기준으로 제약 + 후보 + 근거 + blockers(부족조건)**를 제공하는 것입니다.

## 2-1. 온톨로지 카탈로그를 “추천/검증 가능한 형태”로 내리기
온톨로지를 단순 label/property 목록이 아니라 아래처럼 **기계가 사용 가능한 카탈로그**로 관리합니다.

- labels[]
  - label: Part/Supplier/Drawing/Project
  - keys: merge key 후보(예: part_no_norm)
  - properties[]: {name, type, required, aliases[], examples[]}
- relations[]
  - rel_type: SUPPLIED_BY / CONSISTS_OF …
  - from_label, to_label
  - properties[]: {name, type, required, aliases[], examples[]}
  - endpoint_requirements: from/to에 필요한 key 정의

### aliases(동의어/대체 레이블)
- “특정 속성 하드코딩”이 아니라, 온톨로지 메타데이터로 `aliases`를 제공하는 방식입니다.
- SKOS의 `skos:altLabel`을 참고하면, 다국어/동의어 관리에 적합합니다.
  - SKOS Reference: https://www.w3.org/TR/skos-reference/
  - SHACL처럼 검증 리포트의 severity/근거를 구조화하는 방식도 추천됩니다.
    - SHACL 1.2 Core: https://www.w3.org/TR/shacl12-core/

## 2-2. 컬럼 프로파일링(LLM 없이 결정 가능, deterministic)
각 source column에 대해 샘플 값으로 아래를 계산합니다.

- header_features: 토큰화(한글/영문), 괄호 단서(예: “(원)”), 단위/통화 힌트
- value_type: int/float/string/date/bool 추정
- numeric_stats: min/max, 소수점 유무, 0 비율, 음수 비율
- currency_hint: “원/₩/KRW” 포함 여부(헤더/값)
- cardinality_hint: unique 비율(키 후보), null 비율

## 2-3. 후보 생성 알고리즘(노드 속성/관계 속성을 동일한 방식으로)
후보는 “노드 속성 후보”와 “관계 속성 후보”를 같은 파이프라인으로 생성합니다.

### (1) 타입 호환성 필터
- 컬럼이 numeric이면 ontology의 numeric property(노드/관계 모두)를 후보로 포함
- 통화 힌트가 있으면 cost/price 계열(aliases 포함)을 추가 가중치

### (2) 레키컬 매칭 스코어(aliases 기반)
- column header 토큰 ↔ property name/aliases 토큰 유사도
- 다국어(한글/영문) 매핑은 aliases로 해결(코드 하드코딩 최소화)

### (3) 컨텍스트 스코어
- 같은 파일에 supplier 후보 컬럼(공급처, 업체명, vendor_code)이 존재하고,
  - 실제로 Supplier endpoint로 매핑되어 있으면
  - `SUPPLIED_BY.*` 후보 점수를 올립니다.

### (4) 실행 가능성(feasibility) + blockers 산출
관계 속성 후보의 경우, “관계가 실행 가능한지”를 검사해서 blockers를 반환합니다.
- 예: `SUPPLIED_BY.unit_cost` 후보인데 Supplier endpoint 컬럼이 없으면:
  - blockers: MISSING_ENDPOINT_MAPPING
  - candidate_columns: [공급처, 업체명, vendor_code] 등

## 2-4. 검증 결과 형식: SHACL 스타일(결과 + severity + path)
SHACL은 validation report에 `sh:Violation`, `sh:Warning`, `sh:Info` 같은 severity를 구조화합니다.
- SHACL 1.2 Core: https://www.w3.org/TR/shacl12-core/

이를 매핑 validate 결과에도 그대로 적용하면:
- UI가 issue를 “어디(path)에서 무엇이 문제인지” 명확히 표시 가능
- suggestions(후보/patch)와 연결하기 쉬움
