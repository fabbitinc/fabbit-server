# AGENTS.md
이 문서는 이 저장소에서 작업하는 에이전트용 실행 가이드입니다.
`docs/design/onboarding/*` 설계와 현재 구현 코드를 기준으로 정리했습니다.

## 1) 프로젝트/아키텍처 요약
- 도메인: 제조업 온톨로지 기반 데이터 지식화 파이프라인
- 플로우: Auth → Upload → Mapping → Synthesis → Activation
- 스택: FastAPI, SQLAlchemy, PostgreSQL+Apache AGE, LLM, S3, pandas
- 패키지/런너: `uv`
- Python: `>=3.12`

현재 API 경로(구현 기준):
- Public: `/api/v1/auth/*`
- Tenant: `/api/v1/uploads/*`, `/api/v1/mappings/*`, `/api/v1/synthesis/*`, `/api/v1/activation/*`
- Health: `/health`

## 2) 레이어 규칙(중요)
- `app/api/v1/**/**_router.py`
  - 라우팅, 스키마 바인딩, DI만 담당
  - 비즈니스 로직/DB 쿼리/외부 호출 직접 구현 금지
- `app/modules/*/service.py`
  - 유스케이스 오케스트레이션, 검증, 상태 전이
- `app/modules/*/repository.py`
  - ORM/SQL 데이터 접근 전담
- `app/infrastructure/*`
  - S3/LLM/AGE 등 외부 시스템 캡슐화

## 3) 테넌트 격리 규칙
- tenant 엔드포인트는 `get_tenant_db`를 사용한다.
- tenant 세션 기본: `SET search_path = tenant_{org_id}, ag_catalog, public`.
- AGE 쿼리는 tenant별 graph/schema 이름을 명시한다.
- tenant 라우터에서 public 세션(`get_db`)을 사용하지 않는다.

## 4) 빌드/실행/테스트 명령어

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

Makefile 기반 실행:
```bash
make dev-start
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

E2E 스모크:
```bash
bash scripts/test_full_flow.sh
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

## 5) 코드 스타일 가이드

### 5.1 Import
- 순서: 표준 라이브러리 → 서드파티 → `app.*`
- 그룹 간 한 줄 공백 유지
- 사용하지 않는 import 제거

### 5.2 포맷팅
- PEP 8 기반 유지
- 긴 인자/콜렉션은 trailing comma + 줄바꿈
- 기존 파일 스타일 우선(불필요한 스타일 변경 금지)

### 5.3 타입
- public 함수/메서드 타입 힌트 필수
- `uuid.UUID` 명시 사용
- Optional은 `X | None` 권장

### 5.4 네이밍
- 파일: 라우터 `*_router.py`, 서비스 `service.py`, 저장소 `repository.py`
- 함수/변수: `snake_case`
- 클래스: `PascalCase`
- 상수: `UPPER_SNAKE_CASE`

### 5.5 DB/ORM
- DB 접근은 SQLAlchemy Session만 사용
- 모델 PK는 `generate_uuid7` 사용
- 인덱스/유니크/FK는 `__table_args__` 중심
- tenant 도메인 모델은 `TenantBase` 상속

### 5.6 예외 처리
- 도메인 오류는 `AppError(message, code)` 사용
- 서비스 레이어에서 의미 있는 에러 code로 변환
- 라우터에서 broad `except Exception` 남용 금지

### 5.7 로깅
- `loguru` 사용
- 토큰/비밀번호/민감정보 로그 금지
- 진행률/상태 전이 이벤트 중심 로깅

## 6) docs 반영 원칙
- `docs/design/onboarding/`와 코드 동기화 유지
- 엔드포인트 변경 시 문서 + `scripts/test_full_flow.sh` 함께 수정
- 문서/코드 충돌 시 코드 기준으로 문서 갱신

## 7) 에이전트 작업 체크리스트
변경 전:
1. 대상 도메인의 router/service/repository 위치 확인
2. public/tenant 경계 확인
3. 요청/응답 스키마 및 AppError code 확인

변경 후:
1. `uv run python -m compileall app`
2. `uv run python -m unittest -q`
3. 필요 시 `bash scripts/test_full_flow.sh`
4. 라우터에 비즈니스 로직이 남지 않았는지 확인

## 8) Cursor/Copilot 규칙 파일 상태
- `.cursor/rules/`: 없음
- `.cursorrules`: 없음
- `.github/copilot-instructions.md`: 없음

따라서 본 `AGENTS.md`를 에이전트 기본 규칙으로 사용합니다.
