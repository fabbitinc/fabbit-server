# Service 작성 규칙

## 역할

- 비즈니스 로직 오케스트레이션 — repository, infrastructure 조합
- 트랜잭션 경계 관리 (`@transactional` + `UnitOfWork`)
- LLM 호출 결과 해석 및 검증

## 트랜잭션 (`@transactional`)

- 쓰기: `@transactional` — UnitOfWork로 commit/rollback 자동 관리
- 읽기: `@transactional(read_only=True)` — rollback만, commit 안 함
- 중첩 호출 시 동일 Session이면 외부 트랜잭션 재사용 (contextvars 기반)
- UnitOfWork (`app.core.uow`): 예외 발생 시 자동 rollback

## 함수 시그니처

- 첫 번째 인자: `db: Session`
- 인증 필요 시: `auth: AuthContext`
- 요청 객체: Pydantic schema 사용
- 모듈 함수로 작성 (클래스 불필요)

## 의존성 규칙

- 자기 도메인 repository: `from app.modules.{domain} import repository as repo`
- 타 도메인 repository: `from app.modules.part import repository as part_repo` (alias 구분)
- infrastructure 직접 import 허용: `S3Client`, `execute_cypher_raw` 등
- 다른 도메인 service import 허용 (cross-domain 호출)
- **API layer를 import하지 않을 것**

## 에러 처리

- 비즈니스 에러는 `AppError` 사용 (`app.core.exceptions`)
- 인프라 에러는 잡아서 `AppError`로 변환하거나 재전파
