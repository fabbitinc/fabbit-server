# 아키텍처 컨벤션 마이그레이션 Handoff

## 목표

모든 도메인을 새로운 비즈니스 레이어 컨벤션에 맞게 전환한다.

**컨벤션 핵심 (`.claude/skills/business-layer-guide/`)**:
```
router → queries/      (읽기) → repo
router → use_cases/    (쓰기) → service → repo
                                   ↓
                            model.register_event()
                                   ↓
                         UoW: collect → publish → commit
                                          ↓
                                    handlers/
```

**레이어 규칙 요약**:
| 레이어 | 트랜잭션 | 의존성 |
|--------|----------|--------|
| queries | `@transactional(read_only=True)` | repo (여러 도메인 가능) |
| use_cases | `@transactional` | service (여러 도메인 가능) |
| service | 없음 (use_case가 관리) | 자기 도메인 repo, infrastructure, 모델 메서드 |
| handlers | 없음 (발행자와 같은 트랜잭션) | `get_active_session()` + 자기 도메인 모델/repo |

**금지 사항**:
- `use_case → infrastructure, repo, 도메인 모델 메서드 직접 호출`
- `service → 타 도메인 service, 타 도메인 repo`
- `router → service 직접 import` (queries/use_cases 경유 필수)
- `service → @transactional` (use_case가 소유)

---

## 현재 진행 상황

### ✅ 완료: Part 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| queries 분리 | ✅ | `app/queries/part/` (6개 query 함수) |
| use_cases 분리 | ✅ | `app/use_cases/part/` (4개: add_drawing, add_files, delete_drawing, delete_file) |
| service 정리 | ✅ | `app/modules/part/service.py` — @transactional 제거, 자기 도메인만 접근 |
| router 전환 | ✅ | `app/api/v1/tenant/part_router.py` — queries + use_cases 패턴 |
| drawing service 정리 | ✅ | `app/modules/drawing/service.py` — @transactional 제거 (단, file_repo 위반 잔존) |

### ✅ 완료: 이벤트 정리

| 항목 | 상태 |
|------|------|
| 테스트용 이벤트 전체 삭제 | ✅ (part, drawing, mapping, synthesis events.py 삭제) |
| 핸들러 정리 | ✅ (drawing/handlers.py, synthesis/handlers.py 삭제) |
| 유지 이벤트 | `FileAttached`, `FileDetached`, `AiUsageLogged` 3개만 |
| 모델 register_event 제거 | ✅ (Part의 FileAttached/FileDetached만 유지) |
| event_registry 정리 | ✅ (file, ai_usage 핸들러만 등록) |
| EVENTS.md 갱신 | ✅ |

### ✅ 완료: 인프라/린터

| 항목 | 상태 |
|------|------|
| 아키텍처 린터 규칙 추가 | ✅ `linter/check_import_rules.py` (규칙 5-7) |
| 스킬 문서 보강 | ✅ `.claude/skills/business-layer-guide/` |

---

## 미완료: 도메인별 마이그레이션 작업

### ✅ 완료: File 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| use_cases 분리 | ✅ | `app/use_cases/file/` (4개: create_file, batch_create_files, complete_file, batch_complete_files) |
| service 정리 | ✅ | `app/modules/file/service.py` — @transactional 제거 (6개 함수) |
| router 전환 | ✅ | `app/api/v1/tenant/file_router.py` — use_cases 패턴 |

---

### ✅ 완료: Mapping 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| queries 분리 | ✅ | `app/queries/mapping/` (4개: preview, validate, list, get) |
| use_cases 분리 | ✅ | `app/use_cases/mapping/` (3개: confirm, update, deactivate) |
| service 정리 | ✅ | `app/modules/mapping/service.py` — @transactional 제거, 쓰기 비즈니스 로직만 유지 |
| cross-domain 위반 해결 | ✅ | `mapping/service.py`에서 `ontology_service`, `ai_usage.service` 직접 import 제거 |
| router 전환 | ✅ | `app/api/v1/tenant/mapping_router.py` — queries + use_cases 패턴 |

---

### ✅ 완료: Project API 제거 + Project 최소 엔티티 유지

| 항목 | 상태 | 파일 |
|------|------|------|
| project API 라우터 제거 | ✅ | `app/api/v1/tenant/project_router.py` 삭제 |
| 앱 라우터 등록 해제 | ✅ | `app/main.py` |
| project 모듈 축소 | ✅ | `app/modules/project/models.py` (`id`, `name` 최소 모델 유지), `app/modules/project/repository.py` (검색 전용) |
| project/folder 서비스·스키마 제거 | ✅ | `app/modules/project/service.py`, `app/modules/project/schemas.py` 삭제 |
| project 관련 단위 테스트 제거 | ✅ | `tests/test_project_tree_service.py` 삭제 |

---

### 🔴 Synthesis 도메인

**현황**: router → service 직접 호출, **가장 심각한 cross-domain 위반** (drawing_repo, part_repo, supplier_repo 직접 import)

**router 엔드포인트** (`synthesis_router.py`):
| 엔드포인트 | 현재 | 전환 |
|------------|------|------|
| `GET /` → `service.list_synthesis_jobs()` | 읽기 | → query |
| `GET /{job_id}` → `service.get_synthesis_job()` | 읽기 | → query |
| `GET /batches/{batch_id}` → `service.get_synthesis_batch()` | 읽기 | → query |
| `POST /` → `service.start_synthesis()` | 쓰기 | → use_case |

**cross-domain 위반**:
- `synthesis/service.py:21` → `from app.modules.drawing import repository as drawing_repo`
- `synthesis/service.py:22` → `from app.modules.part import repository as part_repo`
- `synthesis/service.py:23` → `from app.modules.supplier import repository as supplier_repo`

**특이사항**: `start_synthesis()`는 대규모 오케스트레이션 로직 (~900줄). `_run_synthesis()` 내부에서 여러 도메인 repo를 통해 노드/관계를 직접 생성. 단순 use_case 추출로는 해결 어려울 수 있음 — 아키텍처 설계 판단 필요.

**작업 체크리스트**:
- [ ] `app/queries/synthesis/` 생성 (3개 query: list, get_job, get_batch)
- [ ] `app/use_cases/synthesis/` 생성 (1개: start_synthesis)
- [ ] `synthesis/service.py`에서 @transactional 제거
- [ ] cross-domain 위반 해결 (설계 판단 필요)
- [ ] `synthesis_router.py`에서 service → queries + use_cases 전환

---

### ✅ 완료: Dashboard 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| queries 분리 | ✅ | `app/queries/dashboard/` (1개: get_stats) |
| router 전환 | ✅ | `dashboard_router.py` — queries 패턴 |
| service 정리 | ✅ | `app/modules/dashboard/service.py` — 읽기 함수 제거 (쓰기 로직 없음) |

### ✅ 완료: Supplier 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| queries 분리 | ✅ | `app/queries/supplier/` (1개: list_suppliers) |
| router 전환 | ✅ | `supplier_router.py` — queries 패턴 |
| service 정리 | ✅ | `app/modules/supplier/service.py` — 읽기 함수 제거 (쓰기 로직 없음) |

### ✅ 완료: Ontology 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| queries 분리 | ✅ | `app/queries/ontology/` (2개: get_ontology_schema, search_nodes) |
| router 전환 | ✅ | `ontology_router.py` — queries 패턴 |
| service 정리 | ✅ | `app/modules/ontology/service.py` — 읽기 함수 제거, 매핑 로직만 유지 |

---

### ✅ 완료: Activation 도메인

| 항목 | 상태 | 파일 |
|------|------|------|
| queries 분리 | ✅ | `app/queries/activation/` (3개: health_check, query_graph, get_starters) |
| service 정리 | ✅ | `app/modules/activation/service.py` — @transactional 제거, `part_repo` import 제거 |
| router 전환 | ✅ | `app/api/v1/tenant/activation_router.py` — queries 패턴 |

---

### 🟠 Auth 도메인 (별도 판단 필요)

**현황**: public 스키마, 테넌트와 다른 세션 관리. router → service 직접 호출.

| 엔드포인트 | 유형 |
|------------|------|
| `GET /site`, `/check-email`, `/check-slug`, `/me` | 읽기 |
| `POST /register`, `/login`, `/refresh`, `/logout`, `/onboarding/complete` | 쓰기 |

**특이사항**: public 스키마 전용, 테넌트 DB와 다른 세션 사용. 컨벤션 적용 범위를 결정해야 함.

**작업 체크리스트**:
- [ ] 컨벤션 적용 여부 결정 (public 스키마는 제외 가능)
- [ ] 적용 시: queries/use_cases 분리

---

### 🟡 Drawing 도메인 (부분 완료)

**현황**: router 삭제됨 (Part 하위로 통합). service는 Part use_case에서 호출됨.

**잔존 위반**:
- `drawing/service.py:21` → `from app.modules.file import repository as file_repo`

**작업 체크리스트**:
- [ ] `drawing/service.py`에서 file_repo 위반 해결

---

## 우선순위 제안

1. ~~**File**~~ — ✅ 완료
2. ~~**Dashboard, Supplier, Ontology**~~ — ✅ 완료
3. ~~**Mapping**~~ — ✅ 완료
4. ~~**Project / Folder API**~~ — ✅ 완료 (Project 최소 엔티티 유지)
5. ~~**Activation**~~ — ✅ 완료
6. **Synthesis** — 가장 복잡, 설계 판단 필요
7. **Auth** — 적용 범위 결정 필요
8. **Drawing** — file_repo 위반만 남음

---

## 참고: 기존 위반 검출 도구

```bash
# 아키텍처 import 규칙 검증
uv run python linter/check_import_rules.py

# 현재 알려진 위반 (린터가 잡는 것):
# - mapping/service.py:19 → ai_usage.service import (규칙 6 위반)
```

## 참고: 스킬/문서 위치

- 비즈니스 레이어 가이드: `.claude/skills/business-layer-guide/`
- 이벤트 목록: `EVENTS.md`
- 린터: `linter/check_import_rules.py`
