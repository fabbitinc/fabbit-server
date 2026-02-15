# 3. Phase 1 제약(관계 생성/삭제 금지) 유지하면서 사용자 편집 가능성 높이기

핵심: **관계 슬롯 존재가 LLM 출력에 종속되면 안 됩니다.**

## 3-1. 관계 생성/삭제 금지 = “관계 슬롯을 항상 존재시키고 상태만 편집”
- Base Ontology에 정의된 관계 타입마다 `relation_mapping_slot`을 **항상 포함**합니다.
- 사용자는 생성/삭제가 아니라 아래만 편집합니다.
  - state: INACTIVE | ACTIVE | INCOMPLETE
  - endpoint(from_columns / to_columns) 수정
  - relation properties/property_types 수정

이렇게 하면 LLM이 `SUPPLIED_BY`를 누락해도, UI에서 해당 슬롯을 열어 `unit_cost`를 붙일 수 있습니다.

## 3-2. LLM 출력은 ‘전체 매핑 생성’이 아니라 ‘템플릿 patch 제안’으로 제한
- 서버가 ontology 기반 기본 템플릿(관계 슬롯 포함)을 생성
- LLM은 템플릿에 대한 “변경 제안(patch)”만 반환
- 서버는 patch 적용 후 validate

Patch 포맷은 RFC 6902(JSON Patch)를 쓰면 표준이고 구현이 쉽습니다.
- RFC 6902: https://datatracker.ietf.org/doc/html/rfc6902
- RFC Editor info: https://www.rfc-editor.org/info/rfc6902

## 3-3. ‘관계 속성 먼저, endpoint는 나중’ 허용(단, INCOMPLETE로 관리)
- 사용자는 `단가(원)`을 `SUPPLIED_BY.unit_cost`로 먼저 붙일 수 있어야 합니다.
- Supplier endpoint는 추후에 채워도 되지만,
  - validate 결과는 INCOMPLETE로 표시하고,
  - synthesis 실행은 막거나(권장) 별도 승인 흐름으로 분기합니다.

## 3-4. 다의성(공급단가/판매단가/내부원가)을 강제하지 않고 처리
- 서버는 후보로 `SUPPLIED_BY.unit_cost`를 제안할 수는 있으나,
- 최종 선택은 사용자가 확정합니다:
  - 관계 속성으로 붙이기
  - ext로 유지
- 사용자가 선택한 의미/근거는 `mapping_annotations`(결정 로그)로 남깁니다.
  - 이건 온톨로지 속성 추가가 아니라 “매핑 의사결정 기록”입니다.
