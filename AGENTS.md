# AGENTS.md
이 문서는 `fabbit/server` 저장소에서 작업하는 에이전트 가이드입니다.
코드와 `docs/design/onboarding/*`를 기준으로 작성했습니다.

## 1) 프로젝트 요약
- 도메인: 제조업 온톨로지 기반 데이터 파이프라인
- 흐름: Auth → Upload → Mapping → Synthesis → Activation
- 스택: FastAPI, SQLAlchemy, PostgreSQL + Apache AGE, LangChain/OpenAI
- 패키지/실행: `uv` (Python 3.12+)

주요 API 경로(구현 기준):
- Public: `/api/v1/auth/*`, `/api/v1/ontology/*`
- Tenant: `/api/v1/uploads/*`, `/api/v1/mappings/*`, `/api/v1/synthesis/*`, `/api/v1/activation/*`, `/api/v1/drawings/*`, `/api/v1/items/*`
- Health: `/health`

## 2) 레이어 규칙
- `app/api/v1/**/**_router.py`: 라우팅, DI, 스키마 바인딩만 담당
- `app/modules/*/service.py`: 유스케이스 오케스트레이션, 검증, 상태 전이
- `app/modules/*/repository.py`: ORM/SQL/Cypher 데이터 접근 전담
- `app/infrastructure/*`: LLM/S3/AGE 등 외부 연동 캡슐화

금지/권장:
- 라우터에 비즈니스 로직 작성 금지
- 서비스에서 SQL 남발 금지(저장소로 이동)
- 새 기능은 기존 레이어 구조를 우선 재사용

## 3) 테넌트 격리 규칙
- tenant 엔드포인트는 `get_tenant_db` 사용
- tenant 세션은 `search_path`를 `tenant_{org_id}, ag_catalog, public`으로 설정
- AGE 쿼리는 tenant별 graph/schema를 명시적으로 사용
- tenant 라우터에서 public DB 세션(`get_db`) 사용 금지

## 4) Build/Run/Migrate 명령어
환경 준비:
```bash
uv sync
docker compose up -d
uv run alembic upgrade head
```
서버 실행:
```bash
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
Makefile:
```bash
make dev-start
make dev-stop
make dev-db-reset
make dev-reset
make openapi
```

## 5) Test/Lint 명령어 (단일 테스트 포함)
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
E2E 스모크:
```bash
bash scripts/test_full_flow.sh
```
정적 검증:
```bash
uv run python -m compileall app
```
Lint/Format (ruff 설치 환경):
```bash
uv run ruff check .
uv run ruff format .
```

## 6) 코드 스타일 가이드
### 6.1 Imports
- 순서: 표준 라이브러리 → 서드파티 → `app.*`
- import 그룹 사이 공백 1줄
- wildcard import 금지
- 미사용 import 제거

### 6.2 Formatting
- PEP 8 기반 유지
- 긴 인자/컬렉션은 후행 콤마 + 줄바꿈
- 기존 파일 스타일을 우선하고 대규모 재포맷 금지

### 6.3 Types
- public 함수/메서드 타입 힌트 필수
- `Optional[T]`보다 `T | None` 우선
- UUID는 `uuid.UUID`를 명시
- 반환 타입은 가능한 구체적으로 작성

### 6.4 Naming
- 파일: 라우터 `*_router.py`, 서비스 `service.py`, 저장소 `repository.py`
- 함수/변수: `snake_case`
- 클래스: `PascalCase`
- 상수: `UPPER_SNAKE_CASE`
- 테스트 이름: `test_<행동>_<조건>` 권장

### 6.5 API Schema
- API 입출력은 Pydantic 스키마로 정의
- 라우터에서 ad-hoc dict 조립 지양
- 필드명 변경 시 하위 호환성/프론트 영향 확인

### 6.6 DB/ORM
- DB 접근은 SQLAlchemy Session만 사용
- 모델 PK는 `generate_uuid7` 사용
- 인덱스/유니크/FK 선언은 `__table_args__` 원칙 준수
- tenant 비즈니스 모델은 `TenantBase` 상속
- AGE 데이터는 ORM 매핑하지 않고 SQL + Cypher로 처리

### 6.7 Error Handling
- 도메인 오류는 `AppError(message, code)` 사용
- 서비스에서 의미 있는 에러 코드로 변환
- broad `except Exception`은 경계 레이어에서만 제한적으로 사용
- 예외 무시 금지(로그 + 실패 상태 반영)

### 6.8 Logging
- 기본 로깅 패턴은 `loguru` 사용
- 민감정보(토큰/비밀번호/개인정보 원문) 로그 금지
- 대용량 payload는 요약/길이 제한 후 기록

### 6.9 Documentation
- 비자명한 의사결정만 주석으로 남기고 자명한 주석은 피함
- 엔드포인트/흐름 변경 시 `docs/design/onboarding/*` 동기화
- 문서와 코드가 충돌하면 코드 기준으로 문서 갱신

## 7) 작업 체크리스트
작업 전:
1. 대상 모듈의 router/service/repository 경계 확인
2. public/tenant 세션 경계 확인
3. 관련 스키마와 테스트 확인
작업 후:
1. `uv run python -m compileall app`
2. 영향 범위 단일 테스트 이상 실행
3. 필요 시 전체 테스트 실행
4. 필요 시 `bash scripts/test_full_flow.sh` 실행
5. 라우터에 비즈니스 로직이 남지 않았는지 점검

## 8) Cursor/Copilot 규칙 파일 상태
다음 파일/디렉터리를 확인했고 현재 저장소에는 없습니다.
- `.cursor/rules/`
- `.cursorrules`
- `.github/copilot-instructions.md`
따라서 에이전트 기본 규칙 문서는 이 `AGENTS.md`입니다.
