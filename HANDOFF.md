# HANDOFF

## Goal

MappingRecord + MappingRevision 분리를 통해 매핑 업데이트 이력 추적 기능 구현.
기존 단일 `mapping_records` 테이블을 identity(Record) + versioned content(Revision)으로 분리하되, API 표면은 기존과 호환 유지.

## Current Progress

### 1. MappingRecord + MappingRevision 분리 — 완료

**MappingRecord** (identity):
- `id`, `name`, `scope`, `is_active`, `usage_count`, `created_at`, `updated_at`
- `upload_id`, `sheet_name`, `original_headers`, `mapping`, `version` 제거 → Revision으로 이동

**MappingRevision** (versioned content):
- `id`, `record_id` (FK), `upload_id` (FK), `version`, `sheet_name`, `original_headers`, `mapping` (JSONB), `usage_count`, `created_at`
- 인덱스: `ix_mapping_revisions_record_id`, `uq_mapping_revisions_record_version` (unique), `ix_mapping_revisions_upload_id`

**변경 파일:**
- `app/modules/mapping/models.py` — Record 축소 + Revision 신규
- `app/modules/mapping/schemas.py` — `MappingUpdateRequest` 추가, `MappingResponse`에 `version` 추가
- `app/modules/mapping/repository.py` — `(record, revision)` 튜플 반환 패턴
- `app/modules/mapping/service.py` — `confirm_mapping`, `update_mapping`, `deactivate_mapping`, `_to_mapping_response`
- `app/api/v1/tenant/mapping_router.py` — `PUT /{id}`, `DELETE /{id}` 추가
- `app/modules/synthesis/repository.py` — MappingRevision JOIN 경유 조회
- `app/modules/synthesis/service.py` — `revision.mapping`/`revision.sheet_name` 사용
- `tests/test_synthesis_start_service.py` — 모킹 업데이트

### 2. Name 유니크 제약 — 완료

- `uq_mapping_records_name` unique index (비활성 포함)
- `repo.exists_by_name()` + `confirm_mapping`/`update_mapping`에서 중복 검사
- 에러 코드: `DUPLICATE_NAME`

### 3. Scope 자동 판별 — 완료

- 프론트에서 scope를 지정하지 않고, 서버가 매핑 내용 기반으로 자동 판별
- `MappingScope(StrEnum)` 정의 (`app/modules/mapping/constants.py`)
  - `PART_LIST`: relation 없음. 파일 업로드만으로 합성 가능
  - `FULL_BOM`: relation 존재 + 모든 대상 노드 merge key 매핑됨. 파일만으로 합성 가능
  - `ROOT_BOM`: relation 존재 + 대상 노드 merge key 미할당. 합성 시 root_part_number 등 추가 입력 필요
- `_determine_scope(mapping)` 함수가 `relation_mappings` + `merge_keys_by_label` 기반 판별
- `MappingConfirmRequest`에서 `scope` 필드 제거

### 4. updated_at 명시적 갱신 — 완료

- `onupdate=func.now()`는 SQLAlchemy가 실제 UPDATE SQL을 발행할 때만 동작
- scope가 동일 값으로 재할당되면 dirty 감지 안 됨 → `updated_at` null로 남는 버그
- `update_mapping`에서 `record.updated_at = datetime.now(timezone.utc)` 명시적 설정

### 5. Preview 스텁 — 주석 처리됨

- `mapping_router.py`의 `preview_mapping` 스텁 코드가 주석 처리된 상태
- `Path`, `json`, `JSONResponse` import 제거됨 (스텁 비활성화에 따라 불필요)
- `sample/mapping_preview_response.json` 파일은 유지 (프론트 참고용)

## What Worked

- `_validate_and_fix_mapping()`이 모든 매핑 정규화의 최종 관문 → `is_extended` 세팅을 여기서만 처리
- `_determine_scope()`를 온톨로지의 `MANUFACTURING_ONTOLOGY.node_labels`에서 merge key를 동적으로 읽음 → 온톨로지 변경 시 자동 반영
- Repository가 `(record, revision)` 튜플을 반환하는 패턴으로 호출자가 명확하게 두 모델 접근

## What Didn't Work / 주의사항

- **린터가 mapping_router.py 변경을 반복 리버트**: import 추가 + PUT/DELETE 엔드포인트를 3회 재적용해야 했음. 린터가 "미사용 import" 또는 스텁 코드 패턴으로 자동 수정하는 것으로 추정
- **`server_onupdate=func.now()`는 DB 트리거를 생성하지 않음**: SQLAlchemy가 "서버에 트리거가 있다"고 가정만 할 뿐 — 실제 트리거 없으면 무의미
- **`onupdate=func.now()`의 한계**: 같은 값 재할당 시 dirty 감지 안 됨 → 명시적 설정 필요
- **TenantBase 모델은 Alembic이 아닌 provisioning의 `create_all()`로 관리**: 개발 환경에서는 `docker compose down -v && docker compose up -d` 후 `alembic upgrade head`

## 프론트 매핑 UI 설계 방향 (이전 세션에서 합의)

칸반 보드 방식:
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

프론트가 참고할 API 데이터:
- `editable_constraints.allowed_part_properties` → 부품 레인 슬롯 목록
- `editable_constraints.merge_keys_by_label` → 각 레인의 필수 슬롯
- `editable_constraints.relation_catalog` → 관계 타입별 from/to 라벨
- `editable_constraints.relation_property_catalog` → 관계 속성 슬롯 목록

## 테스트 상태

- 71 passed, 6 skipped (LLM 의존 테스트)
- `uv run pytest tests/ -v`

## 전체 파이프라인 현황

```
1. 업로드        → 파일 S3 저장                              ✅ 완료
2. 매핑 미리보기  → LLM 헤더 분석 → MappingResult 반환         ✅ 완료
3. 매핑 확정      → MappingRecord + MappingRevision 저장       ✅ 완료
4. 매핑 수정/삭제 → 리비전 관리, soft-delete                   ✅ 완료
5. 합성(인제스션) → 확정된 매핑 기반 Excel 파싱 → AGE 그래프 적재  🔜 다음 작업
```

## Next Steps

1. **합성(인제스션) 파이프라인 구현** — 확정된 매핑(`MappingRevision.mapping`)을 기반으로 Excel 행을 파싱하여 AGE 그래프에 노드/관계 배치 적재. 기존 `app/modules/synthesis/` 모듈 위에 구현
   - scope별 분기: `PART_LIST`(Part 노드만), `FULL_BOM`(파일만으로 전체 BOM), `ROOT_BOM`(root_part_number 추가 입력 필요)
   - `property_mappings` → Part 노드 속성, `relation_mappings` → 관계 + 대상 노드 생성
   - 확장 속성(`is_extended=true`) → `_ext_` 프리픽스로 노드 속성 저장
2. **DB 재생성 후 검증** — `docker compose down -v && docker compose up -d && uv run alembic upgrade head`
3. **커밋** — Record/Revision 분리 + scope 자동 판별 + updated_at 수정
4. **프론트 매핑 UI** — 칸반 보드 방식으로 재구현 (v2 스키마 기반)
5. **E2E 검증** — 매핑 preview → confirm → update → 합성 → 대시보드 흐름 테스트
