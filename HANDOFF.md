# HANDOFF — Fabbit Server DDD 전환

> 이 문서는 다음 에이전트가 fresh context로 작업을 이어갈 수 있도록 작성되었습니다.
> **마지막 업데이트**: 2026-02-25

---

## Goal

**Service-Repository 패턴 → DDD + Aggregate + Domain Event 패턴으로 점진적 전환**

### 전환 동기 (사용자 확인 완료)

- 모듈 간 결합도 해소 (service가 타 모듈 repository를 직접 import하는 문제)
- 트랜잭션 일관성 (dual-write RDS+Graph 부분 실패 처리)
- LLM 코딩 품질 ("Aggregate 밖은 Event로만 통신"이라는 단순 규칙으로 가드)
- 확장성/유지보수 (새 도메인 추가 시 기존 코드 수정 최소화)

### 전환 범위

**점진적 전환** (사용자 선택):
- Phase 1: Aggregate Base + Event Infra (뼈대 구축, 기존 코드 변경 없이 추가만) ✅
- Phase 2: Part Aggregate 전환 (핵심 도메인)
- Phase 3: 나머지 Aggregate 전환
- Phase 4: import-linter CI 강제

---

## Current Progress

### 완료된 작업

1. **통합 테스트 안전망 확보** (커밋 `c835ad5`)
   - 미테스트 엔드포인트: 24/63 → 9/63 (15개 신규 커버)
   - 남은 9개는 LLM 전용(7) + Drawing 의존(2) — 의도적 제외
   - 72개 테스트 전체 통과 (`make test`)
   - 예외 테스트 10개 추가 (404, 중복 가입, 잘못된 비밀번호, 미인증)

2. **아키텍처 분석 및 전환 전략 합의**
   - Python에서의 DDD 강제 수단 분석 완료 (import-linter CI + model_validator 런타임)
   - Java 전환 불필요 결론 (Python으로 충분히 강제 가능)
   - 4-Phase 점진적 전환 전략 합의

3. **Phase 1: DDD 인프라 뼈대 구축** 완료
   - `app/core/mixins.py` — PkMixin, TimestampMixin, UpdatableMixin, SoftDeleteMixin
   - `app/core/domain_event.py` — DomainEvent base class (Pydantic frozen)
   - `app/core/aggregate.py` — AggregateRoot mixin (이벤트 수집)
   - `app/core/event_bus.py` — 동기 EventBus (in-process 싱글턴)
   - `app/core/uow.py` 수정 — commit 후 Aggregate 이벤트 수집 → EventBus 발행
   - 기존 모델은 AggregateRoot를 상속하지 않으므로 기존 코드 영향 없음

### 아직 시작하지 않은 작업

- [ ] Aggregate 경계 정의 (어떤 엔티티가 어떤 Aggregate에 속하는지)
- [ ] Domain Event 목록 정의

---

## What Worked

- **테스트 커버리지 보강 방식**: 기존 sequential flow 테스트에 새 테스트를 흐름 순서에 맞게 삽입
- **파일 연결 테스트**: 배치 파일(owner_type=project)은 Part에 attach 불가 → 별도 owner 없는 파일을 업로드하여 해결
- **예외 테스트**: logout 이후에도 JWT access_token은 만료 전까지 유효하므로, 예외 테스트를 logout 뒤에 배치해도 정상 동작

---

## What Didn't Work

- **배치 파일을 Part attach에 재사용 시도**: `owner_id`가 이미 설정된 파일은 `CONFLICT` 에러 발생 (`service.py:222` — "이미 다른 리소스에 연결된 파일" 검증). 반드시 owner가 없는 파일이 필요함.

---

## Next Steps — Phase 2: Part Aggregate 전환

### 1. Aggregate 경계 정의 (설계 먼저)

현재 모듈 구조를 기반으로 Aggregate 후보:

| Aggregate Root | 내부 엔티티 후보 | 논의 필요 |
|---|---|---|
| **Part** | BomLink, PartFile, PartSupplier | BomLink가 Part 안인지 독립 Aggregate인지 |
| **Project** | Folder, ProjectPart | ProjectPart는 Part Aggregate와 겹침 |
| **Drawing** | DrawingAnalysis | DrawingSynthesis는? |
| **Mapping** | MappingRevision(현재 없음) | PUT 시 revision 생성 여부 |
| **Synthesis** | SynthesisJob | 배치와의 관계 |
| **Organization** (public) | User, Membership | auth 모듈 내부 |

### 2. Phase 1 완료 — 생성된 인프라 파일

```
app/core/
├── mixins.py            # PkMixin, TimestampMixin, UpdatableMixin, SoftDeleteMixin
├── aggregate.py         # AggregateRoot mixin (_events 수집)
├── domain_event.py      # DomainEvent base class (Pydantic frozen)
├── event_bus.py          # EventBus (동기 in-process 싱글턴)
└── uow.py               # commit 후 이벤트 수집 → EventBus 발행 (수정)
```

**MRO 규칙 (Phase 2 모델 적용 시):**
```python
class Part(AggregateRoot, UpdatableMixin, PkMixin, TenantBase):  # Mixin → Base 순서
    __tablename__ = "parts"
```

### 3. Python 강제 수단 도입 (Phase 4)

- `import-linter` 패키지 설치 및 설정 (`pyproject.toml` 또는 `.importlinter`)
- 규칙 예: "modules.part.service → modules.project.repository import 금지"
- CI/CD 파이프라인에 `lint-imports` 단계 추가

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
- **Rich Domain Model**: 상태 전이 메서드, 팩토리 메서드 (참고: `app/modules/file/models.py`)
- **온톨로지 SSoT**: `app/modules/ontology/base_ontology.py`

### 주요 명령어

```bash
docker compose up -d                    # PostgreSQL + AGE + MinIO
uv sync                                 # 의존성 설치
uv run alembic upgrade head             # DB 마이그레이션
uv run uvicorn app.main:app --reload    # 서버 실행
make test                               # 통합 테스트 (LLM 제외)
make test-e2e                           # 전체 테스트 (LLM 포함)
```

### 디렉토리 구조

```
app/
├── api/v1/          # 라우터 (public/ + tenant/)
├── core/            # database, transactional, uow, exceptions, config
├── infrastructure/  # age_client, llm_client, s3_client, token_provider
└── modules/         # 11개 도메인 모듈 (activation, auth, dashboard, drawing,
                     #   file, mapping, ontology, part, project, supplier, synthesis)
tests/
├── integration/     # CRUD flow 통합 테스트 (72개)
├── fixtures/        # 테스트 데이터 (CSV, JSON)
└── llm/             # LLM 품질 테스트
```

### Cross-module 의존성 현황 (전환 시 해소 대상)

- `part/service.py` → `file/repository`, `ontology/`, `mapping/repository` import
- `synthesis/service.py` → `part/repository`, `ontology/`, `mapping/repository` import
- `drawing/service.py` → `part/service`, `file/repository` import
- `project/service.py` → `part/repository` import

이 cross-module import들이 DDD 전환 시 Event로 대체될 주요 후보입니다.
