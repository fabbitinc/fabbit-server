## Fabbit Server

제조업 온톨로지 기반 데이터 지식화 파이프라인 서버입니다.

핵심 플로우는 다음과 같습니다.

1. Auth
2. Upload
3. Mapping
4. Synthesis
5. Activation

---

## 기술 스택

- Python 3.12+
- FastAPI
- SQLAlchemy
- PostgreSQL + Apache AGE
- LLM(OpenAI 계열)
- S3 호환 스토리지(R2/MinIO)
- pandas/openpyxl
- 패키지/실행: `uv`

---

## 빠른 시작

### 1) 의존성 설치

```bash
uv sync
```

### 2) 인프라 실행

```bash
docker compose up -d
```

### 3) DB 마이그레이션

```bash
# public 스키마
uv run alembic upgrade head

# 테넌트 스키마 (모든 tenant_* 순회)
uv run alembic -c alembic_tenant.ini upgrade head
```

### 4) 서버 실행

```bash
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 모델 테스트

```bash
uv run pytest tests/llm/ -v -s --use-llm --llm-model=minimax/minimax-m2.5 --llm-runs=3
```

또는:

```bash
make dev-start
```

---

## 주요 API 경로 (현재 구현 기준)

- Public
  - `/api/v1/auth/*`
- Tenant
  - `/api/v1/uploads/*`
  - `/api/v1/mappings/*`
  - `/api/v1/synthesis/*`
  - `/api/v1/activation/*`
- Health
  - `/health`

Swagger 문서:

- `http://localhost:8000/docs`

---

## 마이그레이션

Alembic 트랙이 2개로 분리되어 있습니다.

| 트랙 | 설정 파일 | 대상 |
|------|-----------|------|
| public | `alembic.ini` | `Base` 모델 (organizations, users 등) |
| tenant | `alembic_tenant.ini` | `TenantBase` 모델 (projects, column_mappings 등) |

### revision 생성

```bash
# public
uv run alembic revision --autogenerate -m "설명"

# tenant
uv run alembic -c alembic_tenant.ini revision --autogenerate -m "설명"
```

### 마이그레이션 적용

```bash
# public
uv run alembic upgrade head

# tenant (모든 tenant_* 스키마 순회)
uv run alembic -c alembic_tenant.ini upgrade head
```

---

## 테스트/검증 명령어

### test2 구조 (권장)

`test2`는 아래 정책으로 분리되어 있습니다.

- `test2/unit`: 코드 구조와 동일한 단위 테스트
- `test2/e2e/core`: 서버 API core 시나리오 테스트
- `test2/e2e/external`: 외부 API 실호출 테스트
- `test2/llm_eval`: 모델/프롬프트 평가(성능/품질), 기본 실행 제외

실행 예시:

```bash
# unit
uv run pytest test2/unit -c test2/pytest.ini -q

# e2e core
uv run pytest test2/e2e -c test2/pytest.ini -v

# e2e external (실외부 호출)
uv run pytest test2/e2e/external -c test2/pytest.ini -v --use-llm -m "e2e and external and costly"

# llm eval (비용/시간 큼)
uv run pytest test2/llm_eval -c test2/pytest.ini -v -s --use-llm --llm-runs=3 -m "llm_eval and costly"
```

`make` 단축 명령:

```bash
make test2-unit
make test2-e2e
make test2-e2e-external
make test2-llm-eval
```

전체 테스트:

```bash
uv run python -m unittest -q
```

단일 파일 테스트:

```bash
uv run python -m unittest tests.test_tenant_db_dependency
```

단일 클래스 테스트:

```bash
uv run python -m unittest tests.test_tenant_db_dependency.TenantDependencyTests
```

단일 메서드 테스트:

```bash
uv run python -m unittest tests.test_tenant_db_dependency.TenantDependencyTests.test_get_tenant_db_sets_search_path_from_auth_org_id
```

최소 정적 검증:

```bash
uv run python -m compileall app
```

Lint/Format(도구 설치 시):

```bash
uv run ruff check .
uv run ruff format .
```

---

## 디렉터리 개요

```text
app/
  api/
    deps.py
    v1/
      public/
      tenant/
  core/
  infrastructure/
  modules/
docs/
  design/
    onboarding/
scripts/
```

---

## 문서

온보딩/설계 문서는 아래를 참고하세요.

- `docs/design/onboarding/_index.md`
- `docs/design/onboarding/01_02_auth_setup.md`
- `docs/design/onboarding/03_data_upload.md`
- `docs/design/onboarding/04_ai_mapping.md`
- `docs/design/onboarding/05_syntehsis.md`
- `docs/design/onboarding/06_activation.md`

---

## 참고

- 에이전트 작업 가이드는 `AGENTS.md`를 사용합니다.
