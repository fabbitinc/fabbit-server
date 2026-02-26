# queries/ 작성 규칙

## 역할

- 읽기 전용 조회 — router의 유일한 읽기 진입점

## 위치

- `app/queries/{domain}/`

## 트랜잭션

- `@transactional(read_only=True)` — rollback만, commit 안 함

## 호출 대상

- repo 직접 호출 (service 경유 안 함, 여러 도메인 repo 가능)

## 규칙

- 데이터 변경 금지
- 도메인 모델 우회 가능 — DTO 직접 조립 OK
- 복잡한 조회 로직(조인, 집계)을 여기서 처리
