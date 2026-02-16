# HANDOFF — BOM 업로드 설계 v2 구현

## Goal

BOM 파일의 다양한 양식(Flat BOM, Part List, Manual Root BOM)을 하나의 매핑/합성 파이프라인으로 처리할 수 있도록 매핑 스키마, 모델, synthesis 로직을 재설계하고 구현한다.

## 현재 진행 상태

- [x] 문제 식별 및 DB 증거 수집
- [x] 코드 레벨 원인 분석
- [x] BOM 파일 유형 분류
- [x] 설계 v2 작성 + 상세 토론 완료 (모든 미결 사항 해소)
- [x] **코드 수정 완료** — 전체 테스트 60 passed, 4 skipped

## 완료된 작업

### 1. base_ontology.py — CONSISTS_OF 속성 required 제거
- `CONSISTS_OF`의 모든 속성을 `required=False`로 변경 (quantity 포함)

### 2. ontology/schemas.py — Part 속성 / 외부 관계 이분법
- 기존 3분법(ColumnMapping/RelationMapping/ExtendedPropertyMapping) → 2분법
- **PropertyMapping**: `{source_column, target_property, data_type, confidence, reason}` — 행의 주인공 Part 속성
- **RelationMapping**: `{rel_type, target_label, node_columns, rel_columns, rel_column_types, confidence, reason}` — 외부 관계 + 상대방 노드
- `MappingResult`: `{property_mappings: list[PropertyMapping], relation_mappings: list[RelationMapping]}`

### 3. mapping/models.py — MappingRecord 확장
- `scope: str` (master | part_detail), `version: int`, `is_active: bool` 추가
- 인덱스: `ix_mapping_records_scope_is_active`

### 4. part/models.py — BomLink 슬림화
- `sequence`, `reference_designator`, `find_number` 컬럼 제거
- `extended_properties: JSONB`로 이동 (GIN 인덱스 추가)

### 5. mapping/service.py — 새 스키마 기반 검증
- `validate_mapping()`, `confirm_mapping()` 새 스키마에 맞게 재작성

### 6. synthesis/service.py — 완전 재작성
- 기존 from/to 휴리스틱 함수 6개 제거
- 새 데이터 추출 함수: `_extract_row_part`, `_extract_related_parts`, `_merge_part_props`, `_extract_bom_data`
- `_run_synthesis` 청크 루프를 5-phase 구조로 재구성:
  1. 데이터 수집 및 Part별 집계 (first-non-null)
  2. Part upsert (RDS + Graph dual-write)
  3. 비-Part 노드 (Graph only)
  4. BOM 링크 (RDS + Graph dual-write)
  5. 비-CONSISTS_OF 관계 (Graph only)

### 7. part/repository.py — Part 단위 upsert
- `upsert_part()`: 변경 감지(standard/extended 속성 비교), PartRevision 스냅샷
- `upsert_bom_link()`: sequence/ref_des/find_number 제거, extended_properties 지원

### 8. 부수 변경
- `activation/service.py`: `_build_extended_hints`에서 `property_mappings` + `_ext_` 필터 사용
- `scripts/llm-eval/run_mapping_repeat_eval.py`: 새 스키마 형식 적용
- `tests/fixtures/hierarchical_bom_mapping.json`: 새 스키마 형식으로 변환
- `part/schemas.py`: BomChild/BomParent에서 sequence/ref_des/find_number 제거
- `part/service.py`: 제거된 BomLink 필드 참조 정리
- `tests/test_synthesis_start_service.py`: 새 함수명 및 스키마에 맞게 수정

### 9. Alembic 마이그레이션
- 테넌트 스키마는 `TenantBase.metadata.create_all()`로 관리되므로 별도 마이그레이션 불필요
- DB 재생성(`docker compose down -v && up -d`) 후 프로비저닝 시 자동 적용

## What Worked

- "Part 속성 / 외부 관계" 이분법이 from/to 혼동을 구조적으로 제거
- 5-phase 청크 처리가 Part별 집계와 의존성 순서를 명확히 보장
- first-non-null 병합이 중복 행 문제를 깔끔히 해결
- BomLink의 선택적 속성을 extended_properties로 통합하여 모델 슬림화

## What Didn't Work / 주의사항

- `_extract_part_data()`의 from/to 휴리스틱은 완전 제거됨 — 새 코드는 스키마 기반 소속
- 테스트에서 old schema(`column_mappings`, `extended_properties`)를 사용하는 곳이 일부 남아있으나 기능상 문제 없음 (mock으로 MappingResult 파싱을 우회하는 테스트)
- 테넌트 스키마 변경 시 기존 DB는 재생성 필요 (개발 단계)

## Next Steps

1. **DB 재생성** — `docker compose down -v && docker compose up -d && uv run alembic upgrade head && uv run python seed_data.py`
2. **E2E 검증** — 3가지 BOM 유형 샘플 파일로 매핑 → 합성 → 조회 흐름 검증
   - `sample/hierarchical_bom.csv` (Flat BOM)
   - `sample/messy_bom.csv` (Part List)
   - `sample/Arduino_Uno_R3_From_Scratch - 시트1.csv` (전자부품 BOM)
3. **업데이트 토글** — synthesis 시작 시 update_existing 옵션 (현재 항상 ON)
4. **결과 화면** — 신규/변경/스킵 건수 및 충돌 감지 리포트 API
5. **Manual Root BOM** — Part 상세화면에서 scope=part_detail 업로드 흐름
