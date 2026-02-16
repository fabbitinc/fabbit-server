# HANDOFF — Synthesis/Part 설계 리팩토링

## Goal

BOM 파일 synthesis 시 Part 데이터가 잘못 처리되는 근본적인 설계 문제를 해결해야 한다.
현재 BOM 행의 모든 컬럼을 Part 속성으로 upsert하는 방식이 BOM 파일의 본질(관계 데이터)과 충돌한다.

## 발견된 문제들

### 1. BOM 행의 컬럼 소속 혼동

BOM 한 행에는 3가지 성격의 데이터가 섞여 있다:

| 소속 | 컬럼 예시 | 현재 동작 |
|------|----------|----------|
| 상위 Part (식별만) | 상위품번, 상위품명 | ✗ 하위 속성(재질 등)까지 상위 Part에 적용됨 |
| 하위 Part (속성 포함) | 하위품번, 하위품명, 재질, 공급업체 | ✗ 같은 part_number 여러 행 시 마지막 값으로 덮어씀 |
| 관계 (BomLink) | 수량, 단위, 비고 | ○ 정상 동작 |

**원인 코드**: `synthesis/service.py`의 `_extract_part_data()` — from/to 어느 쪽에도 배정 안 되는 속성(재질 등)은 **양쪽에 복사**:
```python
# synthesis/service.py:404-408
else:
    for r in ("from", "to"):
        grouped_props.setdefault((label, r), {})[cm.target_property] = formatted
```

**DB 증거** (`sample/hierarchical_bom.csv` 기준):
- ASM-001(상위 조립품)의 material이 행마다 SS400→AL6061-T6→SUS304→SS400로 변경됨
- 모두 하위 부품의 재질인데 상위 Part에 덮어씌워짐

### 2. PartRevision 중복 생성

같은 part_number가 여러 행에 나오면 행마다 `upsert_part()` → `changed=True` → `_create_revision_snapshot()` 호출.

**DB 증거** (Arduino BOM 데이터):
- Part 38개, PartRevision 531개 (7회 synthesis)
- `C0805C104K1RACAUTO`: job당 6개 revision (6개 행에 등장, 매 행마다 name/ref_designator가 달라 변경 감지)
- 2번째 이후 synthesis에서도 매번 73개 revision 생성 (동일 데이터인데도)

**원인**: 행 단위 upsert이므로, 마지막 행에서 최종 상태 도달 → 다음 synthesis 실행 시 첫 행이 다시 최종 상태를 변경 → `changed=True`

### 3. MappingRecord 중복 confirm

`POST /api/v1/mappings/confirm`에 유니크 제약이 없어 같은 매핑을 여러 번 confirm하면 매번 새 MappingRecord INSERT. 데이터 정합성 문제는 아니지만 UX 이슈.

### 4. 미해결 설계 질문들

사용자가 제기한 근본 질문:

- **BOM 파일은 관계 데이터인데, Part 마스터 속성을 여기서 추출하는 게 맞는가?**
- Part 테이블 데이터가 안 바뀌어도 Drawing이 바뀌면 revision이어야 하는데, 현재 PartRevision은 Part 컬럼 스냅샷일 뿐
- Excel 병행 사용 시 기존 Part와 신규 업로드의 merge 전략 (덮어쓰기 vs 충돌 감지)
- synthesis가 일회성 초기 적재인지, 반복적 동기화인지 정의 필요

## 현재 진행 상태

- [x] 문제 식별 및 DB 증거 수집 완료
- [x] 코드 레벨 원인 분석 완료
- [ ] 설계 방향 결정 (사용자와 논의 필요)
- [ ] 코드 수정 미착수

## 방향성 선택지 (사용자와 논의 중)

### A) BOM synthesis는 관계 중심으로 축소
- Part는 merge key(part_number) + name 정도만 upsert
- material, supplier 등은 BomLink 확장 속성 또는 별도 Part 마스터 업로드로 분리
- 가장 안전하지만, "BOM에서 Part 정보를 자동 추출"하는 기존 가치가 줄어듦

### B) 매핑 단계에서 컬럼 소속 명시적 지정
- "이 컬럼은 상위 Part / 하위 Part / 관계" 구분을 매핑 UI에서 명확히
- 현재 from/to 휴리스틱 대신 사용자가 직접 지정
- 매핑 스키마(MappingResult) 변경 필요

### C) Part 마스터와 BOM 분리
- Part 속성은 Part 목록 파일에서, BOM은 관계 파일에서 각각 synthesis
- 파일 형태별 다른 전략 적용

## 관련 핵심 파일

| 파일 | 역할 |
|------|------|
| `app/modules/synthesis/service.py` | synthesis 메인 로직 (`_run_synthesis`, `_extract_part_data`, `_process_row_nodes`) |
| `app/modules/part/repository.py` | `upsert_part()`, `upsert_bom_link()`, `_create_revision_snapshot()` |
| `app/modules/part/models.py` | Part, PartRevision, BomLink 모델 |
| `app/modules/mapping/service.py` | `confirm_mapping()`, `validate_mapping()` |
| `app/modules/mapping/models.py` | MappingRecord 모델 (유니크 제약 없음) |
| `app/modules/ontology/base_ontology.py` | 온톨로지 SSoT (노드/관계/속성 정의) |
| `app/modules/ontology/schemas.py` | MappingResult, ColumnMapping 스키마 |

## 검증용 DB 쿼리

```sql
-- 테넌트 스키마 (sample BOM 데이터)
SET search_path TO "tenant_019c6035eb2578e181b53d40eff0f66d", public, ag_catalog;

-- 테넌트 스키마 (Arduino BOM 데이터)
-- 이전 테넌트는 tenant_019c5fff... 였으나 테이블이 삭제됨

-- Part별 revision 수 확인
SELECT p.part_number, count(pr.id) as rev_count
FROM parts p LEFT JOIN part_revisions pr ON pr.part_id = p.id
GROUP BY p.part_number ORDER BY rev_count DESC;

-- ASM-001 material 변화 추적
SELECT pr.name, pr.material, pr.created_at
FROM part_revisions pr JOIN parts p ON p.id = pr.part_id
WHERE p.part_number = 'ASM-001' ORDER BY pr.created_at;
```
