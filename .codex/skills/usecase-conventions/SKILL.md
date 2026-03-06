---
name: usecase-conventions
description: Java Spring UseCase 레이어 규칙을 적용한다. 유스케이스 네이밍, Command/Result 경계, 실행 메서드, 트랜잭션 경계 정리가 필요할 때 사용한다.
---

# UseCase Conventions

## 목표

- UseCase를 애플리케이션 시나리오 실행 계층으로 유지하라.
- 입력/출력 경계를 `Command/Result`로 명확히 관리하라.
- 이 스킬은 UseCase 규칙만 다루고 다른 레이어 세부 규칙은 다루지 마라.

## 네이밍 규칙

- 클래스: `*UseCase`
- 입력 모델: `*Command`
- 출력 모델: `*Result` 또는 `void`
- 실행 메서드: `execute(...)`

## 레이어 규칙

- 하나의 UseCase는 하나의 사용자 시나리오를 책임지게 하라.
- 입력은 불변 `Command`(record)를 기본으로 사용하라.
- 출력은 API 독립적인 `Result`를 사용하라.
- UseCase 클래스 레벨에 `@Transactional`을 선언하라.
- UseCase 메서드에는 트랜잭션 어노테이션을 중복 선언하지 마라.
- UseCase에서 HTTP/Swagger 표현(`Request/Response`, `ResponseEntity`)을 다루지 마라.

## 동시성 및 락 선택 규칙

- 일반 수정, 상태 전이, 사용자 편집 충돌 제어는 `@Version` 기반 낙관적 락을 기본으로 사용하라.
- `LockModeType.PESSIMISTIC_WRITE`는 같은 Aggregate를 동시에 선점하면 안 되는 시나리오에서만 예외적으로 사용하라.
- 위 예외 시나리오 예시는 작업 claim, 배치 중복 처리 방지, 한 번에 한 요청만 처리돼야 하는 선점성 갱신이다.
- 락 방식 선택은 UseCase에서 결정하라. Entity가 락 모드를 선택하게 하지 마라.
- `PESSIMISTIC_WRITE`를 사용할 때는 락 획득 직후 엔티티 상태 전이를 수행하고 빠르게 커밋하라.
- `PESSIMISTIC_WRITE` 구간에서는 외부 API 호출, 파일 처리, 긴 계산, 대기성 로직을 넣지 마라.
- 여러 Aggregate를 잠글 필요가 있으면 UseCase 단위로 잠금 순서를 고정해 데드락 가능성을 줄여라.
- 낙관적 락 충돌, 락 대기 timeout, 락 획득 실패 시 처리 정책(재시도, 실패, 에러 매핑)을 UseCase 단위로 명시하라.

## 이벤트 발행 규칙

- 필요 시 UseCase는 이벤트를 발행할 수 있다.
- 이벤트는 시나리오 완료 시점(의미 있는 상태 전환 이후)에 발행하라.
- `동기 이벤트`: 같은 트랜잭션에서 즉시 처리하고 실패 시 요청을 실패/롤백한다.
- `비동기 이벤트`: 트랜잭션을 분리해 후속 처리하며 eventual consistency를 허용한다.
- 이벤트 실패 정책(재시도/보상/중단)을 유스케이스 단위로 명시하라.

## 적용 절차

1. 시나리오 이름을 먼저 확정하고 `*UseCase`를 생성하라.
2. 입력을 `*Command`, 출력을 `*Result`(또는 `void`)로 정의하라.
3. `execute` 메서드에서 시나리오 흐름을 순서대로 구성하라.
4. UseCase 클래스에 `@Transactional`을 선언하라.
5. 이벤트가 필요하면 동기/비동기 방식을 결정하고 실패 정책을 명시하라.
6. 동시성 요구가 있으면 `@Version` 기본, `PESSIMISTIC_WRITE` 예외 원칙으로 락 방식을 선택하라.
7. 유스케이스 시그니처에서 웹 계층 타입 의존성을 제거하라.

## 빠른 체크리스트

- 클래스명이 `*UseCase`로 끝나는가?
- 입력/출력이 `*Command/*Result` 규칙을 따르는가?
- `execute` 메서드가 시나리오 단위를 명확히 표현하는가?
- UseCase 클래스에 트랜잭션이 선언됐는가?
- 동시성 제어 방식이 `@Version` 기본, `PESSIMISTIC_WRITE` 예외 원칙을 따르는가?
- `PESSIMISTIC_WRITE` 사용 시 락 구간이 짧고 외부 I/O가 제거돼 있는가?
- UseCase 코드에 웹 계층 타입이 없는가?
- 이벤트를 발행한다면 동기/비동기 선택 이유와 실패 정책이 명시됐는가?
