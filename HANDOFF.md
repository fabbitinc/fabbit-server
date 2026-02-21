# HANDOFF

## Goal

Part 도메인 조회 API 완성 + 리비전 시스템 구축 + 모듈 구조 정리.
이전 세션에서 dual-write 전환이 완료된 상태에서, Part 목록/상세/필터 API와 리비전 관리를 구현하는 단계.

## Current Progress

### 완료: Part 목록 검색/필터 API

**`GET /api/v1/parts`** — 목록 조회 (페이징 + 필터):
- 필드: `id`, `part_number`, `name`, `category`, `revision`, `lifecycle_state`, `drawing_number`, `children_count`
- Drawing LEFT JOIN (`Part.drawing_id` → `Drawing.drawing_number`)
- BomLink COUNT 상관 서브쿼리 (`children_count`)
- 필터: `search` (품번/품명 ILIKE), `category`, `lifecycle_state` (정확 일치), `has_drawing`, `has_children` (boolean)
- offset/limit 페이징 + total 카운트

**`GET /api/v1/parts/filter-options`** — 필터 옵션 조회:
- `categories`: Part.category DISTINCT 값
- `lifecycle_states`: Part.lifecycle_state DISTINCT 값

**변경 파일:**
- `app/modules/part/schemas.py` — `PartSummary` 필드 확장, `PartFilterOptions` 추가
- `app/modules/part/repository.py` — `list_parts_paginated()` 재작성 (JOIN + 서브쿼리), `get_distinct_categories()`, `get_distinct_lifecycle_states()` 추가
- `app/modules/part/service.py` — `list_parts()` 필터 파라미터 확장, `get_filter_options()` 추가
- `app/api/v1/tenant/part_router.py` — `filter-options` 엔드포인트, 필터 쿼리 파라미터 추가

### 완료: Part 상세/BOM 트리 API — path param을 id(UUID)로 전환

- `GET /api/v1/parts/{part_id}` (기존 `{part_number}` → `{part_id}`)
- `GET /api/v1/parts/{part_id}/bom-tree` (동일)
- service에서 `repo.get_by_id()` 사용, bom-tree는 내부에서 `part.part_number`로 Graph 쿼리
- 테스트 2개 파일 업데이트 (목록에서 id 맵 구축 후 상세 호출)

### 완료: 리비전 시스템 (PartRevision = SoT, Part = 최신 비정규화)

**설계 결정:**
- PartRevision이 SoT (모든 변경 기록), Part는 최신 리비전의 비정규화
- 모든 속성 변경 = 새 리비전 생성
- revision 초기값: incoming(엑셀) 또는 "1", 이후 패턴 감지 기반 자동 증분
- revision status(draft/released)는 향후 추가 예정 (현재 미구현)
- `lifecycle_state`와 revision status는 별개 개념 (전자=제품 수명, 후자=변경 승인)
- `lifecycle_state`는 enum 아닌 자유 문자열 유지 (고객마다 값 상이)

**모델 변경 (`app/modules/part/models.py`):**
- `Part.revision`: `nullable=True` → `nullable=False, server_default="1"`
- `PartRevision.revision`: `nullable=True` → `nullable=False`
- `PartRevision.__table_args__`: `UniqueConstraint("part_id", "revision")` 추가

**Repository 변경 (`app/modules/part/repository.py`):**
- `next_revision(current: str) -> str` 추가 — 패턴 감지 기반 자동 증분
  - 숫자: `"1"→"2"`, `"003"→"004"`, `"Rev.3"→"Rev.4"`
  - 알파벳: `"A"→"B"`, `"Rev.A"→"Rev.B"`, `"Z"→"AA"`
  - 구분자 무관: `.`, `-`, 공백 등 모두 지원
- `_PART_STANDARD_ATTRS`에서 `"revision"` 제거 (시스템이 별도 관리)
- `upsert_part()` 리스트럭처링:
  - 신규 Part: 생성 + `_create_revision_snapshot()` (첫 리비전 기록)
  - 기존 Part+변경: 적용 → `next_revision()` 증분 → `_create_revision_snapshot()` (새 리비전 기록)
- `_create_revision_snapshot()`: `job_id: uuid.UUID | None`으로 변경

**스키마 변경 (`app/modules/part/schemas.py`):**
- `PartSummary.revision`, `PartDetailResponse.revision`: `str | None` → `str = "1"`

### 완료: Drawing 모델 모듈 이동

- `app/modules/document/models.py`의 `Drawing` 클래스 → `app/modules/drawing/models.py`로 이동
- import 경로 4개 업데이트: `drawing/repository.py`, `part/repository.py`, `project/repository.py`, `project/models.py`
- `app/modules/document/` 디렉토리 삭제

### 이전 세션 완료 항목 (요약)

- DEFINED_BY / SUPPLIED_BY dual-write 전환
- nodes/search 검색 범위 확장
- Supplier repository value/label 버그 수정

## What Worked

- conditions 리스트 패턴으로 count/data 쿼리 간 필터 로직 중복 방지
- 상관 서브쿼리(`scalar_subquery`)로 children_count를 N+1 없이 해결
- `next_revision()`의 정규식 `^(.*?)(\d+|[A-Z])$` — 구분자 무관하게 접미사만 증분, 프리픽스 보존
- 리비전 시스템을 B방식(리비전 우선)으로 전환 — A방식(아카이브) 대비 이력 완전성 확보
- Drawing 모델 이동 시 `drawing/models.py`에 이미 다른 모델(DrawingAnalysisRecord, DrawingSynthesisJob)이 있어 합치기만 하면 됨

## What Didn't Work / 주의사항

- **TenantBase 모델 변경은 Alembic이 감지 못함**: DB 재생성 필요 (`docker compose down -v && docker compose up -d` → `uv run alembic upgrade head`)
- **Part.revision NOT NULL 전환**: 기존 데이터가 있으면 마이그레이션 필요하지만, 현재 개발 단계라 DB 재생성으로 해결
- **Part JSONB 통합 계획은 폐기됨**: material, category 등 전용 컬럼을 extended_properties로 통합하는 계획이 있었으나 "현재 상태 유지"로 결정

## 테스트 상태

- 74 passed, 6 skipped
- `uv run pytest tests/ -v`

## 관계 저장 현황

| 관계 | 방식 | RDS 저장 |
|------|------|----------|
| `CONSISTS_OF` (Part→Part) | `upsert_bom_link` | `bom_links` 테이블 |
| `HAS_ITEM` (Project→Part) | project_repo | `project_parts` 테이블 |
| `DEFINED_BY` (Part→Drawing) | `link_part_to_drawing` | `parts.drawing_id` FK (N:1) |
| `SUPPLIED_BY` (Part→Supplier) | `link_part_to_supplier` | `part_suppliers` 테이블 (M:N) |

## Next Steps

### 1. DB 재생성 (모델 변경 반영)

```bash
docker compose down -v && docker compose up -d
uv run alembic upgrade head
```

### 2. 커밋

미커밋 변경사항:
- Part 목록 검색/필터 API
- 리비전 시스템 (PartRevision = SoT)
- Drawing 모델 모듈 이동 (document → drawing)
- Part 상세/BOM 트리 path param UUID 전환
- Supplier 모듈 신규 (untracked)

### 3. 확장 속성 필터 (미구현)

HANDOFF.md 이전 버전에 설계가 있었던 항목:
- `GET /api/v1/parts?ext=material:eq:SUS304` — JSONB 기반 확장 속성 필터
- `ExtendedPropertyDefinition` 자동 등록 (합성 시)
- `GET /api/v1/parts/filter-options` 확장 (extended 속성의 select/range/toggle 옵션)
- 현재는 기본 속성(category, lifecycle_state) 필터만 구현됨

### 4. 리비전 Status 워크플로우 (향후)

- PartRevision에 `status` 필드 추가 (draft → in_review → released)
- 승인 워크플로우 구현 시 함께 도입
- `lifecycle_state`(제품 수명)와는 별개 개념
