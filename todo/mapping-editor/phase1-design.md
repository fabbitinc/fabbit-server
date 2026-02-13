# Mapping Editor Phase 1 설계

## 0. 목표/비목표

### 목표
- 사용자가 화면에서 매핑을 안전하게 수정할 수 있게 한다.
- 관계 추가/삭제 없이도 실무 수정 요구(컬럼 변경, 제외, base/ext 전환, endpoint 재지정)를 해결한다.
- 잘못된 매핑은 confirm 전에 서버가 차단한다.

### 비목표
- 관계 타입 신규 생성/삭제
- 임의 노드/관계 구조 확장
- 전문가 JSON 자유편집

## 1. 편집 권한 범위

### 허용
- ColumnMapping
  - target property 변경
  - 제외/복원
  - data_type 변경
- Base ↔ ExtendedProperty 전환
- RelationMapping
  - 기존 relation의 `from_columns`/`to_columns` source 변경
  - 기존 relation의 `properties` source 변경

### 금지
- relation 추가/삭제
- rel_type 변경
- self-loop에서 동일 source column을 from/to에 동시 지정

## 2. API 설계

## 2.1 Preview 응답 확장

`POST /api/v1/mappings/preview` 응답에 아래 필드 추가:

- `editable_constraints`
  - `allowed_labels`: 온톨로지 node labels
  - `allowed_properties_by_label`: 라벨별 속성명
  - `allowed_rel_types`: 온톨로지 relation types
  - `merge_keys_by_label`: 라벨별 merge key
  - `relation_edit_mode`: `existing_only`

## 2.2 Validate API 신규

`POST /api/v1/mappings/validate`

요청:
- `upload_id: UUID`
- `sheet_name: str | null`
- `mapping: MappingResult`

응답:
- `normalized_mapping: MappingResult`
- `errors: list[ValidationIssue]`
- `warnings: list[ValidationIssue]`
- `impact_summary`
  - `disabled_column_count`
  - `changed_relations_count`
  - `base_to_ext_count`
  - `ext_to_base_count`

`ValidationIssue`:
- `code: str`
- `severity: "error" | "warning"`
- `message: str`
- `path: str` (예: `relation_mappings[0].from_columns.part_number`)

## 2.3 Confirm 강화

`POST /api/v1/mappings/confirm` 처리 시:
1) 내부적으로 validate 실행
2) `errors`가 하나라도 있으면 `INVALID_MAPPING`
3) `normalized_mapping`만 저장

## 3. 서버 검증 규칙

### 오류(error)
- merge key 누락
- 존재하지 않는 source column 참조
- self-loop에서 from/to source 중복
- ontology에 없는 label/property/rel_type 사용
- `_ext_` prefix 누락(extended property)
- required 관계 속성 누락

### 경고(warning)
- integer/float 타입인데 값이 숫자 파싱 불가 패턴
  - 예: `2 EA`, `약 3kg`
- 주요 관계가 모두 비활성화되어 합성 의미가 약해지는 경우

### dismissed_reason 코드
- `missing_from_endpoint`
- `missing_to_endpoint`
- `missing_required_rel_property`
- `missing_source_column`
- `invalid_ext_property_name`

## 4. 프론트 화면 설계

## 4.1 화면 구성

좌측: 원본 컬럼 목록
- 컬럼별 상태: base/ext/excluded

중앙: 노드 속성 매핑 테이블
- 컬럼, 라벨, 속성, 타입, 상태 표시

우측: 관계 매핑 테이블(읽기+제약 편집)
- rel_type은 고정
- endpoint source만 편집 가능
- 추가/삭제 버튼 없음

## 4.2 상호작용

- 컬럼 변경 즉시 로컬 검증 + 영향 배지 표시
- 저장 전 validate 호출
- error 존재 시 Confirm 버튼 비활성화
- warning 존재 시 사용자 확인 모달 후 진행
- restore/toggle은 revalidate 성공 후에만 상태 반영 (optimistic update 금지)

## 5. 데이터 모델 변경점(백엔드)

- `mapping/schemas.py`
  - `MappingPreviewResponse`에 `editable_constraints` 추가
  - `MappingValidateRequest/Response` 추가
- `mapping/service.py`
  - `validate_mapping(...)` 신규
  - `confirm_mapping(...)` 내부 validate 연계
- `mapping_router.py`
  - `POST /api/v1/mappings/validate` 추가

## 6. 테스트 전략

### 단위 테스트
- validate 규칙별 케이스
  - self-loop 중복
  - merge key 누락
  - ext naming 위반
  - 숫자 파싱 경고

### API 테스트
- validate 성공/실패
- confirm에서 validate 실패 차단

## 7. 롤아웃

1) 백엔드 validate + 제약정보 응답 추가
2) 프론트 편집 UI 연결 (관계 추가/삭제 버튼 없음)
3) 내부 사용자로 messy BOM 검증
4) 에러/경고 코드 튜닝

## 8. 리스크 및 완화

- 리스크: 사용자가 "관계 추가"를 기대하지만 불가
  - 완화: 화면에 "Phase 2 예정" 안내 문구 명시
- 리스크: 기존 프론트가 validate 호출을 누락할 수 있음
  - 완화: confirm에서 서버 validate 강제
