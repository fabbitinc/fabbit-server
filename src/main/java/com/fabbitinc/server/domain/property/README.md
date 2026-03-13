# Property Domain

## 목적

`property` 도메인은 사용자 정의 속성과 시스템 속성의 운영 오버라이드를 관리한다.

현재 방향은 아래처럼 나눈다.

1. 시스템 속성의 기본 구조는 코드에서 관리한다.
2. 시스템 속성의 이름 변경/비활성화/표시 순서는 DB override로 관리한다.
3. 커스텀 속성 정의는 `property_definitions`에서 관리한다.

## 현재 구조

### 1. 시스템 속성

시스템 속성의 기본 목록은 코드의 `SystemPropertyRegistry`가 관리한다.

- 파일: `support/SystemPropertyRegistry`
- 각 항목은 `SystemPropertySpec`
- 포함 정보
  - `owner_type`
  - `property_key`
  - `display_name`
  - `description`
  - `value_type`
  - `option_mode`
  - `options`
  - `column_name`
  - `display_order`
  - `required`

즉 시스템 속성의 본체는 더 이상 `property_definitions` row가 아니다.

### 2. 시스템 속성 오버라이드

시스템 속성의 가변 운영 정보는 `system_property_overrides` 테이블에서 관리한다.

현재 오버라이드 대상:

- `display_name_override`
- `display_order`
- `is_active`

이 테이블은 기본 시스템 속성을 다시 정의하지 않는다.
기본 정의는 registry에서 읽고, 운영 오버라이드만 DB에서 덮어쓴다.

`property_key`가 존재하지 않는 override row는 조회 시 무시하는 방향을 전제로 한다.

### 3. 커스텀 속성 정의

커스텀 속성은 `property_definitions` 테이블에서 관리한다.

- `owner_type`
- `display_name`
- `description`
- `value_type`
- `option_mode`
- `options_json`
- `display_order`
- `is_required`
- `is_active`

커스텀 속성은 시스템 속성과 달리 별도 `property_key`를 가지지 않는다.
실제 값 저장 식별자는 계속 `property_definition.id`를 사용한다.

### 4. 옵션형 속성

옵션형 속성은 다음 두 필드로 표현한다.

- `value_type = OPTION`
- `option_mode = FIXED | CREATABLE`

옵션 목록은 `options_json`에 저장하고, 도메인에서는 `PropertyOptionItem`으로 다룬다.

```java
public record PropertyOptionItem(
        String value,
        String label,
        Integer displayOrder,
        Boolean active
) {
}
```

의미:

- `FIXED`
  - 미리 정의된 옵션만 선택 가능
- `CREATABLE`
  - 기존 옵션 선택 가능
  - 새 옵션도 추가 가능

예를 들어 `PART.category`는 registry에서 `OPTION + CREATABLE`로 정의한다.

## 왜 이렇게 나누는가

초기에는 시스템 속성까지 `property_definitions` row로 넣는 방향을 검토했다.
하지만 이 방식은 아래 문제가 있었다.

- 시스템 컬럼 migration과 `property_definitions` seed를 항상 같이 맞춰야 함
- 엔티티 annotation/consistency test 같은 보조 장치가 추가로 필요함
- 시스템 속성과 커스텀 속성이 실제로는 다른 수명주기를 가지는데 모델이 과하게 통합됨

현재 구조는 그 복잡도를 줄이기 위한 타협이다.

- 시스템 기본 구조: 코드
- 시스템 운영 변경: override table
- 커스텀 속성 정의: property_definitions

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

## 추후 확장 구조

조회/필터/분석 요구가 커지면 커스텀 속성 값 저장만 EAV로 분리한다.

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

핵심 원칙:

- 시스템 속성은 계속 컬럼에 남길 수 있다
- 커스텀 속성만 EAV로 분리한다
- 커스텀 속성의 식별자는 계속 `property_definition.id`다

## 구현 원칙 요약

- 시스템 속성의 기본 구조는 registry에서 관리한다.
- 시스템 속성의 rename/deactivate/order는 `system_property_overrides`에서 관리한다.
- 커스텀 속성 정의는 `property_definitions`에서 관리한다.
- 커스텀 속성 값의 실제 식별자는 `property_definition.id`다.
- 옵션은 `value/label`로 분리한다.
- 옵션 생성 가능 여부는 `option_mode`로 표현한다.
