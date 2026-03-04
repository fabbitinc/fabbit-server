---
name: entity-conventions
description: Java Spring Rich Domain Model/DDD 기준으로 Entity 규칙을 적용한다. Aggregate 경계, 상태 전이, 생성 방식, 메서드 네이밍, 예외 정책, 엔티티 테스트, 이벤트 사용 기준 정리가 필요할 때 사용한다.
---

# Entity Conventions

## 목표

- Entity가 도메인 규칙과 상태 전이를 직접 보장하게 하라.
- UseCase/Service로 도메인 규칙이 새지 않도록 엔티티 책임을 명확히 하라.
- 이 스킬은 Entity 규칙만 다루고 다른 레이어 세부 규칙은 다루지 마라.

## Aggregate 경계

- Aggregate는 분리 운영을 기본으로 한다.
- 현재 기준 예시: `Project`, `ProjectMember`, `ProjectPart`를 별도 Aggregate로 둔다.
- 다른 Aggregate 참조는 객체 참조 대신 ID 참조를 우선하라.

## 생성 및 상태 전이 규칙

- 엔티티 생성은 `static factory`를 사용하라.
- 생성자는 제한 접근으로 두고 외부에서 직접 생성하지 마라.
- 상태 전이는 엔티티 메서드 내부에서 직접 강제하라.
- 상태 전이 메서드는 의도 동사를 사용하라 (`changeX`, `assignX`, `archive`, `activate`, `remove`).
- `setX` 형태의 공개 상태 변경 메서드를 금지한다.

## Lombok 및 동등성 규칙

- 엔티티 Lombok은 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만 허용한다.
- 엔티티 동등성은 ID 기반으로만 판단한다.

## 값 타입(VO) 규칙

- 현재 단계에서는 VO를 도입하지 않고 primitive를 유지한다.

## 예외 규칙

- 불변식 위반은 엔티티에서 예외를 발생시켜라.
- 도메인에서는 `DomainException`을 직접 사용하라.
- 도메인 예외 코드(예: `ISSUE_INVALID_STATE`)와 메시지는 `DomainException`에 담아라.
- `AppException`과 API `ErrorCode` 매핑은 UseCase/Service/Presentation에서 처리하라.

## 불변식 및 중복 요청 규칙

- 불변식(허용 상태/금지 상태)은 엔티티 메서드에서 검증하라.
- 동일 요청 반복 처리 정책(no-op vs 예외)은 엔티티별로 명시하고 테스트로 고정하라.

## 이벤트 사용 기준

- 이벤트는 상태 변경 이후의 도메인 반응 전파가 필요할 때 사용하라.
- 즉시 검증/의사결정(권한, 쿼터 허용 여부)은 이벤트 대신 동기 호출을 사용하라.
- 같은 트랜잭션에서 반드시 함께 성공/실패해야 하는 후속 처리는 동기 이벤트를 사용할 수 있다.
- 후처리(알림, 통계, 인덱싱)처럼 분리 가능한 작업은 비동기 이벤트를 사용하라.

## 빠른 체크리스트

- 생성이 `static factory`를 통해서만 이뤄지는가?
- 공개 `setX`가 없는가?
- 상태 전이가 엔티티 메서드에서 강제되는가?
- 엔티티 동등성이 ID 기반으로 정의되는가?
- 불변식 위반이 `DomainException`으로 표현되는가?
- Application 계층에서 `DomainException -> AppException + ErrorCode`로 매핑되는가?
- 엔티티 테스트 최소 기준 4개를 충족하는가?
