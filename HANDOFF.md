# Handoff: Part/BOM Excel Export 기능

## Goal

부품 마스터 화면에서 Part 목록과 BOM 트리를 Excel(.xlsx)로 내보내는 기능 구현.
ERP 자재마스터 등록용이 주 용도이며, 매핑이 있으면 원본 헤더명을 사용한다.

## Current Progress

### 완료된 작업

**1. Part 목록 Export — `GET /api/v1/parts/export`**
- `app/modules/part/repository.py` — `list_parts_for_export()` 추가
- `app/modules/part/service.py` — `export_parts_excel()` 추가
- `app/api/v1/tenant/part_router.py` — 엔드포인트 추가
- 필터: search, category, lifecycle_state, has_drawing, has_children, part_ids
- `mapping_id` optional — 있으면 property_mappings 역매핑으로 원본 헤더명 사용
- 확장 속성: Part들의 extended_properties 키 합집합 수집 → ExtendedPropertyDefinition.display_name 사용
- 매핑 있을 때 컬럼 순서: mapping의 property_mappings 순서 → 나머지 확장 속성
- 매핑 없을 때 컬럼 순서: part_number → name → revision → ... → 확장 속성 알파벳순

**2. BOM 트리 Export — `GET /api/v1/parts/{part_id}/bom/export`**
- `app/modules/part/service.py` — `export_bom_excel()`, `_flatten_bom_tree()` 추가
- `app/api/v1/tenant/part_router.py` — 엔드포인트 추가
- `get_bom_tree()`와 동일한 Graph 경로 조회 → 트리 빌드 로직 재사용
- `_flatten_bom_tree()`로 트리를 level 포함 flat rows로 재귀 펼침
- 파라미터: part_id (path), direction (forward/reverse), mapping_id (optional)
- 컬럼: level, part_number, name, revision, quantity, material, unit, category, lifecycle_state
- 매핑 역매핑: property_mappings + CONSISTS_OF rel_columns

**3. 공통 — 열 너비 자동 조정**
- `_auto_fit_columns(ws)` 헬퍼 — 데이터 최대 길이 기반 너비 설정 (min 8, max 50)
- Part export, BOM export 양쪽에 적용

**4. 한글 파일명 처리**
- `Content-Disposition` 헤더에 한글 파일명 → RFC 5987 방식 (`filename*=UTF-8''...`) 사용
- `urllib.parse.quote()` 적용

## What Worked

- 기존 `list_parts_paginated()`의 필터 조건을 `list_parts_for_export()`에 중복 작성 (단순하고 독립적)
- BOM export에서 `get_bom_tree()`의 내부 함수(`_build_bom_tree`, `repo.get_bom_paths` 등)를 직접 재사용
- openpyxl의 `column_dimensions[letter].width`로 열 너비 수동 설정 (autofit 미지원)

## What Didn't Work

- **전역 BOM 링크 export (`/bom/export`)**: 처음에 전체 bom_links를 search/part_ids로 필터하는 방식으로 구현했으나, 사용자 의도는 특정 Part의 BOM 트리 export (`/{part_id}/bom/export`)였음. `list_bom_links_for_export()` 포함 전부 제거하고 재구현함.
- **`.limit(10_000)` 안전장치**: 사용자가 모르는 암묵적 상한은 혼란만 줌. 제거함.
- **한글 filename 직접 사용**: `Content-Disposition` 헤더는 latin-1 인코딩만 허용. RFC 5987 `filename*` 필요.

## Next Steps

- [ ] Swagger UI에서 실제 데이터로 두 엔드포인트 테스트
  - Part export: 파라미터 없이 / category 필터 / mapping_id 포함
  - BOM export: 특정 part_id / direction=reverse / mapping_id 포함
- [ ] 다운로드된 엑셀 열어서 확장 속성 컬럼, 한글 헤더, 열 너비 확인
- [ ] 프론트엔드에서 export 버튼 연결 (Part 목록 화면, BOM 트리 화면)

## 관련 파일

| 파일 | 변경 내용 |
|------|-----------|
| `app/modules/part/repository.py` | `list_parts_for_export()` 추가 |
| `app/modules/part/service.py` | `export_parts_excel()`, `export_bom_excel()`, `_flatten_bom_tree()`, `_auto_fit_columns()` 추가 |
| `app/api/v1/tenant/part_router.py` | `GET /parts/export`, `GET /parts/{part_id}/bom/export` 추가 |
