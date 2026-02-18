# HANDOFF

## Goal

이번 세션에서 3가지 작업을 진행했다:
1. 대시보드 통계 API 구현
2. PropertyMapping에 `is_extended` 플래그 추가
3. 샘플 JSON을 v2 스키마로 업데이트

## Current Progress

### 1. Dashboard Stats API — 완료

- [x] `app/modules/dashboard/` 모듈 신규 생성 (schemas, repository, service)
- [x] `GET /api/v1/dashboard/stats` 라우터 + `app/main.py` 등록
- [x] 전체 테스트 71 passed

**응답 구조:**
```json
{
  "parts": { "total": 42, "added_this_week": 5 },
  "bom_links": { "total": 120 },
  "last_synthesis": { "job_id": "uuid", "status": "COMPLETED", "completed_at": "...", "nodes_created": 15, "relationships_created": 30 }
}
```

### 2. PropertyMapping `is_extended` 플래그 — 완료

- [x] `ontology/schemas.py` — `PropertyMapping`에 `is_extended: bool = False` 추가
- [x] `ontology/service.py` — `_validate_and_fix_mapping()`에서 자동 세팅
  - `_ext_*` 접두사 또는 온톨로지에 없는 속성 → `is_extended=True`
  - 표준 속성 → `is_extended=False`
- [x] `display_name` 필드는 불필요하여 제거 (source_column이 이미 Excel 헤더명)

**프론트 사용법:**
- `is_extended=true` → `source_column`을 레이블로 사용
- `is_extended=false` → 온톨로지 스키마에서 레이블 조회

### 3. 샘플 JSON v2 업데이트 — 완료

- [x] `sample/mapping_preview_response.json` — v2 스키마로 변환
- [x] `sample/mapping_preview_messy.json` — v2 스키마로 변환

**v1 → v2 주요 변경:**
- `column_mappings` + `extended_properties` → `property_mappings` (`is_extended`로 구분)
- `from_label/to_label/from_columns/to_columns` → `target_label/node_columns/rel_columns`
- `editable_constraints` → `allowed_part_properties`, `relation_catalog`, `relation_property_catalog`

## What Worked

- `_validate_and_fix_mapping()`이 모든 매핑 생성/정규화의 최종 관문이라 `is_extended` 세팅을 여기서만 처리하면 전체 파이프라인에 적용됨
- `confirm_mapping`이 저장 전에 `normalize_mapping`을 호출하므로 DB에는 항상 `is_extended` 포함된 상태로 저장됨

## What Didn't Work / 주의사항

- `display_name` 필드를 처음에 추가했다가 제거함 — `source_column`이 이미 Excel 원본 헤더명이라 중복
- 프론트 매핑 UI가 v1 스키마 기준으로 구현되어 있어 전면 재작업 필요 (아래 Next Steps 참고)

## 프론트 매핑 UI 설계 방향 (합의됨)

현재 프론트 문제:
- 관계 추가 폼이 v1의 from/to 패턴 그대로
- CONSISTS_OF만 지원, SUPPLIED_BY/DEFINED_BY 추가 불가
- 상대방 노드의 전체 속성을 다 나열 (merge key만 필수인데 10개 필드 노출)

**합의된 칸반 보드 방식:**
```
[부품]              [상위 부품]         [공급사]          [도면]
 품번 *              품번 *              업체명 *          도면번호 *
 품명                품명               ─ 관계 속성 ─
 재질               ─ 관계 속성 ─        단가
 단위                수량
 ...                 순서
─ 확장 속성 ─
 (드롭 시 자동생성)
```

- 각 레인 = 엔티티 (Part, Parent Part, Supplier, Drawing)
- Excel 컬럼 카드를 슬롯에 드래그 → 자동으로 property_mappings 또는 relation_mappings 생성
- merge key(*) 슬롯만 필수, 나머지 선택
- HAS_ITEM(프로젝트) 레인은 실무에서 거의 불필요하므로 당장은 제외

**프론트가 참고할 API 데이터:**
- `editable_constraints.allowed_part_properties` → 부품 레인 슬롯 목록
- `editable_constraints.merge_keys_by_label` → 각 레인의 필수 슬롯
- `editable_constraints.relation_catalog` → 관계 타입별 from/to 라벨
- `editable_constraints.relation_property_catalog` → 관계 속성 슬롯 목록

## Next Steps

1. **커밋** — 현재 미커밋 변경사항 (dashboard API + is_extended + 샘플 JSON)
2. **프론트 매핑 UI** — 칸반 보드 방식으로 재구현 (백엔드 변경 불필요)
3. **E2E 검증** — 서버 + 프론트 연동하여 매핑 → 합성 → 대시보드 흐름 테스트
4. **openapi.json 갱신** — dashboard 엔드포인트 추가 반영
