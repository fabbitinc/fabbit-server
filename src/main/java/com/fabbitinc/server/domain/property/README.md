# Property Domain

## 목적

`property` 도메인은 시스템 속성과 커스텀 속성을 하나의 property catalog로 관리한다.

핵심 원칙:

1. 메타데이터의 단일 source of truth는 `property_definitions`다.
2. 시스템 속성도 `property_definitions` row로 프로비저닝한다.
3. 시스템/커스텀 차이는 `source_type`, `storage_kind`, `storage_binding`으로 표현한다.
4. 조회/정렬/활성화/표시명 변경은 모두 같은 catalog row를 기준으로 처리한다.

## 현재 구조

### 1. 통합 property catalog

모든 속성 메타는 `property_definitions` 테이블에서 관리한다.

- `owner_type`
- `property_key`
- `source_type`
- `storage_kind`
- `storage_binding`
- `part_system_property_kind`
- `display_name`
- `description`
- `value_type`
- `option_mode`
- `options_json`
- `display_order`
- `is_required`
- `is_active`
- `is_active_configurable`

의미:

- `source_type = SYSTEM | CUSTOM`
- `storage_kind = COLUMN | EXTENDED_PROPERTY`
- `storage_binding`
  - 시스템 속성: 실제 엔티티 컬럼명
  - 커스텀 속성: `extended_properties` key

### 2. 시스템 속성

시스템 속성은 더 이상 runtime registry + override merge로 읽지 않는다.

- 코드에는 provisioning seed만 남긴다.
- 테넌트 초기화 시 seed를 기반으로 시스템 속성 row를 보장한다.
- 조회/수정/정렬은 항상 DB row만 읽는다.

따라서 시스템 속성의 최종 메타는 DB에서 바로 확인할 수 있다.

### 3. 커스텀 속성

커스텀 속성도 같은 `property_definitions` row를 사용한다.

- `source_type = CUSTOM`
- `storage_kind = EXTENDED_PROPERTY`
- `property_key`는 UUID 문자열
- `storage_binding`도 동일한 UUID 문자열

기존 `extended_properties` JSONB key는 계속 이 UUID 문자열을 사용한다.

### 4. 값 저장 모델

현재 값 저장 모델은 하이브리드다.

- 시스템 속성 값: 각 엔티티의 실제 컬럼
- 커스텀 속성 값: 각 엔티티의 `extended_properties` JSONB

중요한 점은 메타데이터와 값 저장 모델을 분리해서 본다는 것이다.

- 메타는 통합
- 값 저장은 `COLUMN` / `EXTENDED_PROPERTY`로 공존

## 운영 규칙

### 시스템 속성

- 삭제 불가
- 타입/옵션/필수 여부 변경 불가
- 표시명/설명/표시 순서/활성 여부만 변경 가능
- `is_active_configurable=false`인 항목은 비활성화 불가

### 커스텀 속성

- 생성 가능
- 부분 수정 가능
- 사용 중이면 삭제 불가
- 미사용이면 삭제 가능

## API 원칙

- 목록: `GET /api/v1/properties/meta`
- 생성: `POST /api/v1/properties/definitions`
- 수정: `PATCH /api/v1/properties/definitions/{ownerType}/{propertyKey}`
- 삭제: `DELETE /api/v1/properties/definitions/{ownerType}/{propertyKey}`
- 순서 변경: `PATCH /api/v1/properties/order`

`/system-overrides` 같은 별도 시스템 속성 API는 두지 않는다.

## 장기 확장

이 구조는 향후 분석/LLM read model의 기반으로 사용한다.

- 메타데이터 질의는 `property_definitions`
- 값 질의는 `COLUMN`/`EXTENDED_PROPERTY`를 해석하는 별도 read layer

즉 장기 방향은:

- write model: 통합 property catalog
- read model: 분석/LLM 전용 unified property view
