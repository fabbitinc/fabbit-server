# 저장 전략

RDS와 Graph의 역할 분담, dual-write 패턴, 확장 속성 필터링 전략을 정의한다.

---

## 1. 원칙: RDS가 항상 SoT

```
RDS (PostgreSQL)  →  Single Source of Truth
Graph (AGE)       →  탐색 보조 (다단계 경로, 비-BOM 관계)
```

- 모든 비즈니스 데이터의 정합성은 RDS가 보장한다.
- Graph는 가변 길이 경로 탐색(`CONSISTS_OF*`)처럼 RDS로 비효율적인 쿼리에만 사용한다.
- Graph에 장애가 생겨도 RDS만으로 핵심 기능(Part 조회, depth-1 BOM)이 동작해야 한다.

---

## 2. 저장 위치 매트릭스

| 데이터 | RDS | Graph | 비고 |
|--------|:---:|:-----:|------|
| Part 속성 (전체) | O | X | Graph Part 노드에는 `part_number`만 |
| BOM 관계 (depth 1) | O | O | dual-write, RDS가 SoT |
| BOM 경로 (다단계) | X | O | 가변 길이 탐색은 Graph 필수 |
| Drawing 관계 | X | O | 향후 RDS 전환 예정 |
| Supplier 관계 | X | O | 향후 RDS 전환 예정 |
| 확장 속성 값 | O (JSONB) | O (node/edge property) | dual-write |
| 확장 속성 메타 | O (`extended_property_definitions`) | X | RDS 전용 |

---

## 3. Dual-Write 패턴

Part와 BOM 관계는 RDS와 Graph 양쪽에 저장된다. Repository가 이를 캡슐화하며, Service는 저장 위치를 모른다.

### 3.1 Part Upsert (`part_repo.upsert_part`)

```
호출자 → part_repo.upsert_part(db, part_number, props, job_id, graph_name)
            │
            ├── RDS: INSERT/UPDATE parts 테이블 (전체 속성)
            │         + PartRevision 스냅샷 생성
            │
            └── Graph: MERGE (n:Part {part_number: '...'})
                       part_number만 저장 (속성 없음)
```

### 3.2 BOM Link Upsert (`part_repo.upsert_bom_link`)

```
호출자 → part_repo.upsert_bom_link(db, graph_name, parent_pn, child_pn, ...)
            │
            ├── RDS: UPSERT bom_links 테이블
            │         quantity, sequence, ref_designator, find_number
            │         + extended_properties JSONB
            │
            └── Graph: MATCH (a:Part), (b:Part)
                       MERGE (a)-[r:CONSISTS_OF]->(b)
                       SET r.quantity = ..., r.확장속성 = ...
```

### 3.3 조회 경로

| 조회 유형 | 데이터 소스 | 함수 |
|----------|-----------|------|
| Part 상세 (속성) | RDS | `db.query(Part)` |
| 자식/부모 (depth 1) | RDS JOIN | `part_repo.get_children/get_parents` |
| BOM 트리 (다단계) | Graph | `part_repo.get_bom_paths` |
| Drawing/Supplier | Graph | `item_repo.get_drawings/get_suppliers` |

- depth 1 BOM 조회는 RDS JOIN으로 처리. `parts.name`이 JOIN에 포함되므로 별도 enrichment 불필요.
- 다단계 BOM 트리는 Graph의 `CONSISTS_OF*` 가변 길이 탐색을 사용. 결과의 name은 RDS에서 일괄 조회(`_bulk_get_names`).

---

## 4. 확장 속성 저장 전략

### 4.1 값 저장: JSONB

Part와 BomLink 모두 `extended_properties` JSONB 컬럼을 가진다.

```json
// parts.extended_properties 예시
{
  "_ext_carbon_emission": "150",
  "_ext_surface_treatment": "아노다이징",
  "_ext_weight_kg": 2.5
}

// bom_links.extended_properties 예시
{
  "_ext_remark": "2차 벤더 허용",
  "_ext_process_code": "WLD-03"
}
```

합성(synthesis) 시 Base Ontology에 정의되지 않은 속성은 자동으로 `_ext_` 프리픽스가 붙어 JSONB에 저장된다.

### 4.2 메타데이터: extended_property_definitions 테이블

JSONB에 어떤 키가 존재하는지, 표시명과 타입은 무엇인지를 관리하는 레지스트리.

```
합성 시 자동 등록:
  매핑 확정 → extended_properties 항목 → extended_property_definitions UPSERT
  (key, target_entity, data_type은 자동, display_name은 source_column에서 추출)

어드민 편집:
  display_name 수정
```

**API 활용 예시**:
- `GET /extended-property-definitions?target_entity=Part`
  → 프론트에서 동적 필터 패널 생성
- data_type에 따라 필터 위젯 결정 (number → 범위 슬라이더, string → 텍스트, boolean → 토글)

### 4.3 필터링 인덱스

```
parts.extended_properties         → GIN 인덱스 (jsonb_ops)
bom_links.extended_properties     → GIN 인덱스 (jsonb_ops)
```

- GIN 인덱스로 키 존재 검사(`?`), 값 포함 검사(`@>`)를 커버한다.
- 특정 확장속성에 대한 빈번한 범위 쿼리가 발생하면, Expression B-tree 인덱스를 추가로 생성한다:
  ```sql
  -- 예: 탄소배출량 범위 필터가 자주 사용되는 경우
  CREATE INDEX ix_parts_ext_carbon ON parts
  USING btree (((extended_properties->>'_ext_carbon_emission')::float));
  ```
- Expression 인덱스 추가는 `extended_property_definitions.filterable = true`인 항목 기준으로 판단한다.

---

## 5. 테넌트 격리

| 대상 | 격리 방식 |
|------|----------|
| RDS 테이블 (parts, bom_links, extended_property_definitions, ...) | `tenant_{org_id}` 스키마 + `SET search_path` |
| Graph (Part, CONSISTS_OF, ...) | `tenant_{org_id}` 그래프 (AGE graph-per-tenant) |

- 프로비저닝 시 `TenantBase.metadata.create_all()`로 모든 테넌트 테이블 생성.
- `extended_property_definitions`도 테넌트 스키마에 생성되므로 테넌트별 독립적인 확장 속성 세트를 가진다.
