# modules/*/service.py 작성 규칙

## 역할

- 쓰기 비즈니스 로직 — 검증, 상태 전이, 이벤트 발행

## 트랜잭션

- `@transactional` 없음 — use_case가 트랜잭션을 관리
- service는 이미 열린 트랜잭션 안에서 실행됨

## 호출 대상

- 자기 도메인 repo
- infrastructure (S3Client, execute_cypher_raw 등)
- 도메인 모델 메서드 (상태 전이, 팩토리)

## 규칙

- 자기 도메인 repo만 직접 호출
- 타 도메인 service import 금지 — 순환 참조 방지, 크로스 도메인은 use_case에서 조합
- 모델 필드 직접 대입 금지 — 도메인 메서드 호출 (`entity.mark_uploaded()`)
- 모델 팩토리 메서드가 있으면 직접 생성(`Model(field=...)`) 대신 팩토리 사용
- 프론트 응답용 schema 변환(URL 생성 등)은 해당 도메인 service가 제공 — use_case가 infrastructure에 직접 의존하지 않도록 감싸기

## 권한 검증 경계

- **RBAC(역할 기반)**: router `Depends(require_admin)` 등에서 처리 → service에서 `if auth.role != ...` 금지
- **비즈니스 규칙**: service에서 처리 (자기 자신 제거 불가, 소유자 보호, 리소스 소유 확인 등 DB 조회 필요한 검증)
