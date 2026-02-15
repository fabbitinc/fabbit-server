# 데이터 모델

Fabbit 서버의 도메인 엔티티, 속성, 관계를 정의한다.

---

## 1. 속성 구분: 기본 vs 확장

모든 엔티티의 속성은 두 계층으로 나뉜다.

| 구분 | 정의 위치 | 저장 위치 | 예시 |
|------|----------|----------|------|
| **기본 속성** | Base Ontology (`base_ontology.py`) + ORM 컬럼 | 테이블 고정 컬럼 | `part_number`, `name`, `quantity` |
| **확장 속성** | 테넌트별 가변 (합성 시 자동 등록) | `extended_properties` JSONB | `_ext_carbon_emission`, `_ext_unit_cost` |

- 기본 속성은 코드에 타입이 정해져 있고, 모든 테넌트에 공통이다.
- 확장 속성은 테넌트마다 다르며, `_ext_` 프리픽스로 식별한다.
- 확장 속성의 메타데이터(표시명, 타입 등)는 `extended_property_definitions` 테이블이 관리한다.

---

## 2. 엔티티 정의

### 2.1 Part (부품)

BOM의 기본 단위. 품번(`part_number`)으로 식별한다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | UUID v7 (PK) | |
| `part_number` | String(100), UNIQUE | 품번 (merge key) |
| `name` | String(500) | 품명 |
| `revision` | String(50) | 리비전 |
| `material` | String(200) | 재질 |
| `unit` | String(20) | 단위 |
| `description` | Text | 설명 |
| `category` | String(100) | 분류 |
| `is_phantom` | Boolean | 팬텀 여부 |
| `lifecycle_state` | String(50) | 수명주기 상태 |
| `lead_time_days` | Integer | 리드타임(일) |
| `extended_properties` | JSONB | 확장 속성 값 |
| `created_at` | DateTime(tz) | |
| `updated_at` | DateTime(tz) | |

### 2.2 BomLink (BOM 관계)

Part 간 CONSISTS_OF 관계. 부모-자식 쌍으로 식별한다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | UUID v7 (PK) | |
| `parent_part_id` | FK → parts.id | 상위 부품 |
| `child_part_id` | FK → parts.id | 하위 부품 |
| `quantity` | Integer, default=1 | 수량 |
| `sequence` | Integer, nullable | 순서 |
| `reference_designator` | String(200) | 참조 지시자 |
| `find_number` | String(100) | 찾기 번호 |
| `extended_properties` | JSONB | 확장 속성 값 |
| `created_at` | DateTime(tz) | |

**제약조건**: `(parent_part_id, child_part_id)` UNIQUE — 동일 부모-자식 쌍 중복 방지.

### 2.3 PartRevision (부품 리비전 스냅샷)

Part의 변경 이력. 합성(synthesis) 작업 시 Part가 생성/수정될 때마다 스냅샷을 남긴다.

- Part의 전체 컬럼을 복제하여 저장 (extended_properties 포함)
- `synthesis_job_id`로 어떤 합성 작업에서 변경되었는지 추적

### 2.4 ExtendedPropertyDefinition (확장 속성 정의)

테넌트별 확장 속성의 메타데이터 레지스트리. 필터 UI 생성, 타입 검증, 표시명 제공에 사용한다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | UUID v7 (PK) | |
| `key` | String(200) | JSONB 내부 키 (예: `_ext_carbon_emission`) |
| `display_name` | String(200) | UI 표시명 (예: `탄소배출량`) |
| `data_type` | String(20) | `string` / `integer` / `float` / `boolean` |
| `target_entity` | String(50) | 소속 엔티티: `Part` / `BomLink` |
| `created_at` | DateTime(tz) | |

**제약조건**: `(key, target_entity)` UNIQUE — 동일 엔티티에 같은 키 중복 방지.

**데이터 소스**: 합성(synthesis) 시 `extended_properties` 항목이 자동 등록. 이후 어드민이 `display_name` 등을 편집.

---

## 3. 관계

### 3.1 BOM 관계 (Part ↔ Part)

```
Part ──[CONSISTS_OF]──> Part
```

- RDS: `bom_links` 테이블 (depth 1 조회 최적화)
- Graph: `CONSISTS_OF` edge (다단계 경로 탐색)
- 양쪽에 동일 데이터가 존재한다 (dual-write). 저장 전략은 `storage_strategy.md` 참고.

### 3.2 비-BOM 관계 (Graph 전용, 현재)

```
Part ──[DEFINED_BY]──> Drawing      (도면)
Part ──[SUPPLIED_BY]──> Supplier    (공급사)
Part ──[MADE_OF]──> Material        (재질)
```

- 현재 RDS 테이블 없이 Graph에만 존재한다.
- Drawing, Supplier, Material은 RDS 모델이 아직 없으므로 Graph 노드로만 관리.
- 향후 이들도 RDS SoT 전환 시 Part와 동일한 패턴(고정 컬럼 + extended_properties JSONB)을 적용한다.

---

## 4. 전체 구조도

```
┌─────────────────────────────────────────────────────┐
│                    RDS (SoT)                        │
│                                                     │
│  parts ──FK──> bom_links <──FK── parts              │
│    │                                                │
│    └─FK─> part_revisions                            │
│                                                     │
│  extended_property_definitions (메타데이터 레지스트리)                │
│                                                     │
│  ┌─────────────┐  ┌─────────────┐                   │
│  │   parts     │  │  bom_links  │                   │
│  │  .extended_ │  │  .extended_ │                   │
│  │  properties │  │  properties │  ← JSONB (값)     │
│  └──────┬──────┘  └──────┬──────┘                   │
│         └────────┬───────┘                          │
│                  ▼                                  │
│          extended_property_definitions        ← 메타 (키, 타입,    │
│          (key, display_name,      표시명, 필터여부)  │
│           data_type, ...)                           │
├─────────────────────────────────────────────────────┤
│                   Graph (보조)                       │
│                                                     │
│  Part ──CONSISTS_OF──> Part     (다단계 탐색용)      │
│  Part ──DEFINED_BY──> Drawing   (RDS 미전환)         │
│  Part ──SUPPLIED_BY──> Supplier (RDS 미전환)         │
│  Part ──MADE_OF──> Material     (RDS 미전환)         │
└─────────────────────────────────────────────────────┘
```
