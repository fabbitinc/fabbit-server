# HANDOFF

## Goal

DEFINED_BY / SUPPLIED_BY 관계를 Graph-only에서 RDS+Graph dual-write로 전환 완료 후,
Part 목록 검색/필터 기능을 설계·구현하는 단계.

## Current Progress

### 완료: DEFINED_BY / SUPPLIED_BY dual-write

**모델 변경 (`app/modules/part/models.py`):**
- `Part.drawing_id` FK 추가 (→ `drawings.id`, `SET NULL`) — N:1 관계
- `PartRevision.drawing_id` 스냅샷 컬럼 추가 (FK 없음)
- `PartSupplier` 조인 테이블 신규 — M:N 관계 (`part_id`, `supplier_id`, `unit_cost`, `extended_properties`)

**Repository 변경 (`app/modules/part/repository.py`):**
- `link_part_to_drawing()` — Part.drawing_id 설정 + Graph DEFINED_BY MERGE
- `link_part_to_supplier()` — PartSupplier upsert + Graph SUPPLIED_BY MERGE
- `get_drawings(db, part_id)` — Graph Cypher → RDS JOIN 전환
- `get_suppliers(db, part_id)` — Graph Cypher → RDS JOIN 전환
- `upsert_part()` — `job_id` Optional 전환 (Drawing 합성에서 `None` 전달)
- `_create_revision_snapshot()` — `drawing_id` 포함

**Service 변경:**
- `app/modules/part/service.py` — drawings/suppliers 조회를 RDS dict 직접 접근으로 변경, Graph 파싱 헬퍼(`_safe_float`) 제거
- `app/modules/synthesis/service.py` — Phase 5를 DEFINED_BY/SUPPLIED_BY dual-write로 분리 (5a/5b/5c)
  - `_process_row_relationships()`에서 DEFINED_BY/SUPPLIED_BY 제외
  - `_extract_defined_by()`, `_extract_supplied_by()` 함수 추가
  - Rootless relation도 DEFINED_BY/SUPPLIED_BY는 구조화 경로로 처리
- `app/modules/drawing/service.py` — `_run_drawing_synthesis`를 `upsert_drawing` + `part_repo.upsert_part` + `link_part_to_drawing` 호출로 전환
  - `_build_drawing_props`, `_build_part_props` — Cypher format → Python dict 반환

**Drawing repository 정리 (`app/modules/drawing/repository.py`):**
- Graph-only 함수 3개 제거: `merge_drawing_node`, `merge_part_node`, `merge_defined_by`

**문서 업데이트:**
- `docs/agents/project.md` — 관계 저장 위치: `DEFINED_BY` → RDS FK + Graph, `SUPPLIED_BY` → RDS 조인 테이블 + Graph
- `docs/agents/repository.md` — dual-write 대상에 DEFINED_BY, SUPPLIED_BY 추가

### 완료: nodes/search 검색 범위 확장

`search_merge_key()` 검색 필드 확장 (반환값은 merge key 유지):
- Part: `part_number OR name`
- Drawing: `drawing_number OR name`
- Supplier: `company_name OR code`
- `ontology_router.py` docstring에 검색 범위 명세 추가

### 완료: Supplier repository value/label 수정

- `search_merge_key()` 반환에서 `value`(merge key)와 `label`이 뒤바뀌어 있던 버그 수정
- `Supplier.code`가 nullable이라 `value`에 들어가면 Pydantic 검증 에러 발생했음

## What Worked

- `upsert_bom_link` 패턴을 그대로 `link_part_to_supplier`에 적용 — 일관성 유지
- Drawing은 N:1이므로 `Part.drawing_id` FK로 충분, 별도 조인 테이블 불필요
- `_process_row_relationships`에서 DEFINED_BY/SUPPLIED_BY를 스킵하고 별도 추출 함수로 분리하는 접근이 깔끔

## What Didn't Work / 주의사항

- **TenantBase 모델 변경은 Alembic이 빈 마이그레이션 생성**: `Base`(public)만 추적하므로 autogenerate가 감지 못함. 개발 환경에서는 DB 재생성 필요: `docker compose down -v && docker compose up -d` → `uv run alembic upgrade head`
- **Supplier.code는 nullable**: merge key가 아닌 필드를 `value`로 반환하면 Pydantic `string` 검증 실패

## 테스트 상태

- 74 passed, 6 skipped
- `uv run pytest tests/ -v`

## 관계 저장 현황 (모두 RDS+Graph dual-write 완료)

| 관계 | 방식 | RDS 저장 |
|------|------|----------|
| `CONSISTS_OF` (Part→Part) | `upsert_bom_link` | `bom_links` 테이블 |
| `HAS_ITEM` (Project→Part) | project_repo | `project_parts` 테이블 |
| `DEFINED_BY` (Part→Drawing) | `link_part_to_drawing` | `parts.drawing_id` FK (N:1) |
| `SUPPLIED_BY` (Part→Supplier) | `link_part_to_supplier` | `part_suppliers` 테이블 (M:N) |

## Next Steps

### 1. Part 목록 검색/필터 구현 (설계 합의 완료)

**API 설계:**

#### `GET /api/v1/parts/filter-options` — 필터 옵션 조회
프론트가 필터 UI를 동적으로 구성하기 위한 메타데이터 반환.

```json
{
  "basic": [
    {"key": "category", "display_name": "분류", "data_type": "string",
     "values": ["기구부품", "전자부품"]},
    {"key": "is_phantom", "display_name": "팬텀 여부", "data_type": "boolean"},
    {"key": "lead_time_days", "display_name": "리드타임(일)", "data_type": "integer",
     "min": 1, "max": 90}
  ],
  "extended": [
    {"key": "_ext_color", "display_name": "색상", "data_type": "string",
     "values": ["RED", "BLUE"]},
    {"key": "_ext_weight", "display_name": "중량", "data_type": "float",
     "min": 0.1, "max": 150.0}
  ]
}
```

- `string` → `values` (DISTINCT 쿼리)
- `integer`/`float` → `min`, `max`
- `boolean` → 필드만 (값 고정)
- 확장 속성 정의는 `ExtendedPropertyDefinition` 테이블에서 조회 (이미 모델 존재)

#### `GET /api/v1/parts` — 목록 조회 (필터 확장)

```
GET /api/v1/parts?search=BRK
    &category=기구부품
    &lifecycle_state=ACTIVE
    &ext=_ext_color:eq:RED
    &ext=_ext_weight:like:100
```

- `search`: `part_number OR name` ILIKE (기존)
- 기본 속성 필터: 쿼리 파라미터 직접 (`eq` 고정)
- 확장 속성 필터: `ext` 반복 파라미터, `key:op:value` 형식
  - `eq` → `extended_properties->>'key' = 'value'`
  - `like` → `extended_properties->>'key' ILIKE '%value%'`

**구현 순서:**
1. `ExtendedPropertyDefinition` 등록 로직 확인 (합성 시 자동 등록되는지)
2. `GET /api/v1/parts/filter-options` 엔드포인트 구현
3. `GET /api/v1/parts` 필터 파라미터 확장
4. 테스트

### 2. DB 재생성 (dual-write 모델 반영)

```bash
docker compose down -v && docker compose up -d
uv run alembic upgrade head
```

### 3. 커밋

- DEFINED_BY / SUPPLIED_BY dual-write 전환
- nodes/search 검색 범위 확장
- Part 목록 필터 (구현 후)
