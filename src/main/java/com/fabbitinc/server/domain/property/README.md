# Property Domain

## 목적

`property` 도메인은 부품/공급사/도면/관계 엔티티가 가질 수 있는 속성의 메타데이터를 관리한다.

현재 목표는 아래 두 가지다.

1. 시스템 기본 속성도 rename, deactivate 대상으로 관리한다.
2. 커스텀 속성을 JSONB로 저장하되, 추후 EAV 구조로 옮길 수 있게 식별 체계를 고정한다.

## 현재 구조

### 1. 메타데이터 원천

현재 속성 정의의 단일 원천은 `property_definitions` 테이블이다.

- 시스템 속성도 row로 가진다.
- 커스텀 속성도 row로 가진다.
- 대상은 `PropertyOwnerType`으로 구분한다.
  - `PART`
  - `SUPPLIER`
  - `DRAWING`
  - `BOM_LINK`
  - `PART_SUPPLIER`

### 2. 시스템 속성

시스템 속성은 실제 컬럼과 연결된다.

- `is_system = true`
- `property_key` 필수
- `column_name` 필수

예시:

- `PART.material`
- `PART.part_number`
- `SUPPLIER.company_name`
- `BOM_LINK.quantity`
- `PART_SUPPLIER.unit_cost`

`property_key`는 시스템 속성의 semantic key다.

- 다국어 처리
- 코드 상 식별
- 외부 계약 고정

을 위해 유지한다.

### 3. 커스텀 속성

커스텀 속성은 실제 테이블 컬럼이 없고, 각 엔티티의 `extended_properties` JSONB에 저장된다.

- `is_system = false`
- `column_name = null`
- `display_name`은 사용자가 보는 이름이다.
- rename은 `display_name` 변경으로 처리한다.

중요한 점은 현재/미래 모두 값 저장 기준은 `property_definition.id`라는 점이다.

- JSONB key도 `property_definition.id`
- 추후 EAV 전환 시 FK도 `property_definition.id`

즉 커스텀 속성용 별도 문자열 key를 추가로 만들지 않는다.

### 4. 옵션형 속성

옵션형 속성은 `PropertyValueType.OPTION`으로 표현한다.

옵션 메타데이터는 현재 `options_json`에 저장한다.

도메인에서는 raw JSON 문자열이 아니라 `PropertyOptionItem`을 사용한다.

```java
public record PropertyOptionItem(
        String value,
        String label,
        Integer displayOrder,
        Boolean active
) {
}
```

현재 규칙:

- 저장은 JSONB
- 애플리케이션 경계에서는 typed model 사용
- 옵션 구조는 `value/label/displayOrder/active`
- `OPTION` 타입이 아니면 옵션 목록을 가질 수 없다
- 같은 속성 내 옵션 `value`는 중복될 수 없다

`value`는 저장용 식별자이고, `label`은 표시용 이름이다.

## 현재 저장 모델

현재 값 저장 모델은 하이브리드다.

- 시스템 속성 값: 각 엔티티의 실제 컬럼에 저장
- 커스텀 속성 값: 각 엔티티의 `extended_properties` JSONB에 저장

예시:

```json
{
  "550e8400-e29b-41d4-a716-446655440000": "도금"
}
```

위 key는 커스텀 속성의 표시명이 아니라 `property_definition.id`다.

이렇게 하는 이유:

- 커스텀 속성 rename 안전
- display name 변경 시 데이터 migration 불필요
- 추후 EAV migration 단순화

## 현재 한계

현재 구조는 상세 조회와 유연한 저장에는 적합하지만, 아래 요구가 커지면 한계가 생긴다.

- 목록 검색
- 다중 필터
- 범위 검색
- 정렬
- 집계/분석

이 요구가 커지면 JSONB만으로는 성능과 인덱싱 전략이 점점 불리해진다.

## 추후 확장 구조

확장 방향은 `property_definitions`를 유지한 채, 값 저장만 EAV로 분리하는 것이다.

### 1. 유지되는 것

- `property_definitions`는 계속 메타데이터 원천으로 사용한다.
- 시스템 속성의 `property_key`는 그대로 유지한다.
- 커스텀 속성의 실제 식별자는 계속 `property_definition.id`다.
- `options_json`도 초기에는 그대로 유지한다.

### 2. 추가될 값 테이블

향후에는 커스텀 속성 값을 별도 EAV 테이블로 분리한다.

예시 스키마:

```sql
property_values
- id uuid pk
- owner_type varchar not null
- owner_id uuid not null
- property_definition_id uuid not null
- string_value text null
- number_value numeric null
- boolean_value boolean null
- option_value varchar null
- created_at timestamptz not null
- updated_at timestamptz not null

unique(owner_type, owner_id, property_definition_id)
```

설명:

- `owner_type + owner_id`로 실제 대상 row를 식별한다.
- `property_definition_id`로 어떤 속성인지 식별한다.
- 값은 타입별 컬럼에 나눠 담는다.
- 옵션형은 `option_value`에 `PropertyOptionItem.value`를 저장한다.

### 3. Migration 방향

JSONB에서 EAV로 갈 때는 아래 순서로 옮긴다.

1. 각 엔티티의 `extended_properties`를 읽는다.
2. JSON key를 `property_definition.id`로 해석한다.
3. `property_values.property_definition_id`에 그대로 넣는다.
4. value type에 맞는 typed column에 값을 분배한다.

핵심은 현재 JSONB key와 미래 EAV FK가 동일하다는 점이다.

이 규칙 덕분에 커스텀 속성 값 migration 시 별도 key 변환 작업이 필요 없다.

### 4. 조회 전략

EAV 도입 후에도 API/화면 관점에서는 시스템 속성과 커스텀 속성을 분리해서 보여주지 않는다.

- 사용자에게는 모두 "속성"이다.
- 내부 저장만 컬럼/EAV로 나뉜다.

즉 응답 모델은 통합하고, 저장 모델만 분리한다.

### 5. 추가 최적화

필터/정렬/분석이 더 무거워지면 EAV만으로도 부족할 수 있다.

그 경우 다음 단계를 고려한다.

- projection/search 전용 테이블
- 속성별 보조 인덱스
- 집계용 materialized view

즉 장기 방향은 다음 순서다.

1. 현재: `property_definitions + JSONB`
2. 중기: `property_definitions + EAV(property_values)`
3. 장기: `property_definitions + EAV + projection/search model`

## 구현 원칙 요약

- 시스템 속성도 `property_definitions`에서 관리한다.
- 시스템 속성은 `property_key`를 가진다.
- 커스텀 속성은 문자열 key를 추가로 만들지 않는다.
- 커스텀 속성 값의 실제 식별자는 `property_definition.id`다.
- 옵션은 `value/label`로 분리한다.
- 저장은 JSONB를 유지하되, 애플리케이션 경계에서는 typed model을 사용한다.
- 추후 EAV 전환 시 `property_definition.id`를 FK로 사용한다.
