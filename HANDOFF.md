# HANDOFF — Fabbit Server DDD 전환

> 이 문서는 다음 에이전트가 fresh context로 작업을 이어갈 수 있도록 작성되었습니다.
> **마지막 업데이트**: 2026-02-25

---

## Goal

**Service-Repository 패턴 → DDD + Aggregate + Domain Event 패턴으로 점진적 전환**

### 전환 동기

- 모듈 간 결합도 해소 (service가 타 모듈 repository를 직접 import하는 문제)
- 트랜잭션 일관성 (dual-write RDS+Graph 부분 실패 처리)
- LLM 코딩 품질 ("Aggregate 밖은 Event로만 통신"이라는 단순 규칙으로 가드)
- 확장성/유지보수 (새 도메인 추가 시 기존 코드 수정 최소화)

---

## Current Progress

### 완료된 작업

1. **통합 테스트 안전망 확보** (커밋 `c835ad5`)
   - 72개 통합 테스트 + 12개 유닛 테스트 전체 통과 (`make test`)

2. **Phase 1: DDD 인프라 뼈대 구축** ✅
   - `app/core/aggregate.py` — AggregateRoot mixin (`register_event()` / `collect_events()`)
   - `app/core/domain_event.py` — DomainEvent base class (Pydantic `frozen=True`)
   - `app/core/event_bus.py` — 동기 EventBus (in-process 싱글턴, `subscribe()` / `publish()`)
   - `app/core/uow.py` — commit 후 Session의 new/dirty에서 AggregateRoot 이벤트 수집 → EventBus 발행
   - `app/core/mixins.py` — PkMixin, TimestampMixin, UpdatableMixin, SoftDeleteMixin (미적용)

3. **Phase 2: Part Aggregate 전환** ✅
   - `Part(AggregateRoot, TenantBase)` — `create()` 팩토리, `update_properties()`, `assign_drawing()`/`unassign_drawing()`
   - `app/modules/part/events.py` — 6개 이벤트 (PartCreated, PartPropertiesUpdated, PartDrawingLinked/Unlinked, PartFileAttached/Detached)

4. **Phase 3: 나머지 Aggregate 전환** ✅
   - **Drawing** — `create_from_upsert()` 팩토리, `update_properties()`, `complete_conversion()`/`fail_conversion()` + 이벤트 3개
   - **DrawingSynthesisJob** — `create()` 팩토리, `start_processing()`/`complete()`/`fail()`, `DrawingSynthesisJobStatus` Enum
   - **SynthesisJob** — `create()` 팩토리, `assign_batch()`/`start_processing()`/`set_total_rows()`/`update_progress()`/`complete()`/`complete_empty()`/`fail()` + 이벤트 3개, `SynthesisJobStatus` Enum
   - **MappingRecord** — `rename()`/`update_scope()`/`deactivate()`/`increment_usage()` + MappingDeactivated 이벤트
   - **File** — `mark_uploaded()`/`mark_deleted()`/`mark_expired()`에 이벤트 발행 추가 (FileUploaded/FileDeleted/FileExpired)
   - Service: 모든 직접 필드 대입 → 도메인 메서드 호출로 전환

5. **Phase 4: 이벤트 핸들러 인프라 완성 + 첫 핸들러 등록** ✅
   - **버그 수정**: `UnitOfWork._collect_aggregate_events()`에서 `session.deleted` 누락 → `new | dirty | deleted` 수정
   - **Background task에서 UoW 사용**: 이벤트 발행이 필요한 최종 상태 전이 commit에서 `UnitOfWork(db).commit()` 직접 사용 — `@transactional`과 동일한 UoW 기반, 새 컨벤션 없음
   - **이벤트 레지스트리**: `app/core/event_registry.py` — 명시적 import로 핸들러 등록, `main.py` startup에서 호출
   - **첫 핸들러**: `synthesis/handlers.py` (SynthesisJobCompleted/Failed 로깅), `drawing/handlers.py` (DrawingConversionCompleted/Failed 로깅)
   - **Background task 적용**: `synthesis/service.py`, `drawing/service.py`의 최종 상태 전이 commit → `commit_and_publish(db)` 교체

6. **Phase 5: AI Usage 로깅 이벤트 전환** ✅
   - **`AiUsageLogged` 이벤트**: `app/modules/ai_usage/events.py` — fire-and-forget side-effect 이벤트
   - **DB-mutating 핸들러**: `app/modules/ai_usage/handlers.py` — 자체 SessionLocal로 public 스키마에 기록 (테넌트 트랜잭션과 독립)
   - **호출부 전환**: 3개 모듈(drawing, mapping, activation) 5개 지점에서 `log_ai_usage()` → `event_bus.publish(AiUsageLogged(...))` 교체
   - **`log_ai_usage()` 함수 삭제**: `ai_usage/service.py`에서 제거, `check_bom_quota()`만 유지
   - **설계 판단**: Aggregate 상태 변경이 아닌 application-level side-effect이므로 `event_bus.publish()` 직접 호출 (register_event→UoW 패턴과 별개)

### 정의된 이벤트 목록

| 모듈 | 이벤트 | 핸들러 |
|------|--------|--------|
| part | PartCreated | — |
| part | PartPropertiesUpdated | — |
| part | PartDrawingLinked | — |
| part | PartDrawingUnlinked | — |
| part | PartFileAttached | — |
| part | PartFileDetached | — |
| drawing | DrawingConversionCompleted | ✅ 로깅 |
| drawing | DrawingConversionFailed | ✅ 로깅 |
| drawing | DrawingPropertiesUpdated | — |
| synthesis | SynthesisJobStarted | — |
| synthesis | SynthesisJobCompleted | ✅ 로깅 |
| synthesis | SynthesisJobFailed | ✅ 로깅 |
| file | FileUploaded | — |
| file | FileDeleted | — |
| file | FileExpired | — |
| mapping | MappingDeactivated | — |
| ai_usage | AiUsageLogged | ✅ DB 기록 (자체 SessionLocal) |

### 아직 시작하지 않은 작업

- [ ] **DB-mutating 핸들러** — 핸들러에서 DB 변경이 필요한 경우의 세션 전략 (tenant_schema 전달 방식) 확정 필요
- [ ] **Cross-module import 정리** — 구체적 비즈니스 요구에 따라 이벤트 기반 side-effect로 전환
- [ ] import-linter CI 강제

---

## Cross-module Import 분석 결론

상세 분석 결과, 즉시 이벤트로 대체할 수 있는 side-effect는 **매우 제한적**이다:

| 유형 | 개수 | 판단 |
|------|------|------|
| **Orchestration** (조회 후 분기) | 9개 | 유지 — 이벤트 대체 불가 |
| **Application Service** (synthesis 다중 Aggregate 조율) | 5개 | 유지 — orchestrator 패턴 |
| **Same-transaction cascade** (삭제 시 파일 정리) | 4개 | 유지 — 트랜잭션 일관성 필요 |
| **Read-only** (상수/Enum) | 3개 | 유지 — 결합도 문제 아님 |
| **자체 세션 로깅** (ai_usage) | 2개 | ✅ Phase 5에서 이벤트 전환 완료 |

→ **cross-module import 제거보다 "인프라 완성 + 확장 기반 마련"이 우선이었고, Phase 4에서 달성.**

---

## What Worked

- **Phase 2 Part 패턴**: `AggregateRoot, TenantBase` MRO 순서, `_STANDARD_ATTRS`를 models.py에 SSoT로 배치, 팩토리+도메인 메서드 패턴 — 나머지 Phase 3에 동일하게 적용 성공
- **테스트 대역**: `_FakeJob` 클래스 (도메인 메서드 구현), `_FakeSession`에 `new`/`dirty` 속성 추가 — UnitOfWork 이벤트 수집과 호환
- **Background task UoW 직접 사용**: `commit_and_publish` 헬퍼 대신 `UnitOfWork(db).commit()`으로 통일 — 새 컨벤션 없이 기존 UoW 메커니즘 재사용

## What Didn't Work

- **git stash pop 실패**: 편집 중 `git stash` 후 `git stash pop`에서 충돌 → stash drop으로 모든 수정 유실. 새 파일(events.py 등)은 untracked라 생존. **편집 중에는 stash 사용 금지**
- **Phase 플랜 설계 미스**: 가장 중요한 "이벤트 핸들러 등록 + cross-module import 제거" 단계가 Phase에 빠져 있었음. Phase 1~3은 인프라+발행 구조만 만들고, 실제 결합도 해소는 미수행

---

## Next Steps

### 1. DB-Mutating 핸들러 세션 전략

현재 핸들러는 로깅만 수행. DB 변경이 필요한 핸들러를 도입하려면:
- 핸들러에 tenant_schema를 어떻게 전달할지 결정 (이벤트에 포함 vs context)
- 핸들러 전용 세션 생성 vs 기존 세션 재사용
- 실패 시 보상 트랜잭션 전략

### 2. import-linter CI 도입

핸들러 등록 후 import 규칙을 강제:
- `import-linter` 패키지 설치 (`pyproject.toml`)
- 모듈 간 허용/금지 규칙 정의
- `make lint` 또는 CI에 `lint-imports` 단계 추가

---

## 프로젝트 핵심 정보

### 기술 스택

- FastAPI + Pydantic, Python 3.12+
- PostgreSQL + Apache AGE (Cypher 그래프 쿼리)
- SQLAlchemy (sync) + Alembic (public/tenant 분리 트랙)
- LangChain + OpenRouter (GPT-5-mini)
- 패키지 관리: uv

### 핵심 패턴

- **Schema-per-Tenant**: `public` + `tenant_{org_id}` 스키마 격리
- **RDS-Graph 듀얼라이트**: Part/Drawing/Supplier는 RDS 전체 속성 + Graph merge key
- **@transactional 데코레이터**: `app/core/transactional.py` (contextvars 기반 세션 스택)
- **Rich Domain Model**: 상태 전이 메서드, 팩토리 메서드
- **온톨로지 SSoT**: `app/modules/ontology/base_ontology.py`

### 주요 명령어

```bash
docker compose up -d                    # PostgreSQL + AGE + MinIO
uv sync                                 # 의존성 설치
uv run alembic upgrade head             # DB 마이그레이션
uv run uvicorn app.main:app --reload    # 서버 실행
make test                               # 통합 테스트 (LLM 제외)
```

### DDD 인프라 파일 구조

```
app/core/
├── aggregate.py         # AggregateRoot mixin (register_event / collect_events)
├── domain_event.py      # DomainEvent base class (Pydantic frozen)
├── event_bus.py         # EventBus 동기 싱글턴 (subscribe / publish)
├── event_registry.py    # 핸들러 등록 레지스트리 (startup 시 호출)
├── uow.py              # UnitOfWork (commit 시 이벤트 수집→발행)
└── mixins.py            # PkMixin, TimestampMixin 등 (미적용)

app/modules/*/events.py    # 모듈별 도메인 이벤트 정의
app/modules/*/handlers.py  # 모듈별 이벤트 핸들러 (ai_usage, synthesis, drawing)
```
