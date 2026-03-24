---
name: test-conventions
description: 테스트 작성 규칙을 적용한다. 단위 테스트/통합 테스트 추가/수정, 테스트 구조 설계, Fixture 설계, 테스트 계층 선택이 필요할 때 사용한다.
---

# Test Conventions

## 목표

- 단위 테스트와 통합 테스트의 역할을 명확히 분리하라.
- 각 계층에서 검증해야 할 것만 검증하라.
- 테스트가 실제 동작과 동일한 경계에서 실행되게 하라.

## 테스트 계층 분리 원칙

| 계층 | 위치 | 검증 대상 | 특징 |
|------|------|-----------|------|
| 도메인 단위 테스트 | `src/test/**/domain/` | 엔티티 상태 전환, 검증 로직, 도메인 규칙 | 순수 Java, Mock 없음 |
| Service 단위 테스트 | `src/test/**/application/` | Service 메서드의 세부 동작, 분기 로직 | Mockito Mock 사용 |
| 통합 테스트 | `src/integrationTest/` | UseCase 조합으로 비즈니스 흐름 검증 | 실제 DB (Testcontainers) |

검증하려는 것이 어느 계층인지 먼저 판단하고 적절한 테스트 유형을 선택하라.

---

## 단위 테스트 규칙

### 도메인 단위 테스트

- 엔티티의 생성, 상태 전환, 검증 로직을 검증하라.
- Spring 컨텍스트를 로드하지 마라. 순수 Java로 작성하라.
- `assertThrows(DomainException.class, ...)`로 도메인 규칙 위반을 검증하라.
- 에러 코드(`ex.getDomainCode()`)까지 검증하라 — 메시지가 아닌 코드로 확인한다.

### Service 단위 테스트

- `@ExtendWith(MockitoExtension.class)`를 사용하라.
- 의존성은 `@Mock`으로 선언하고 Service 인스턴스를 직접 생성하라.
- `when(...).thenReturn(...)`으로 의존성 동작을 정의하라.
- 분기 로직, 예외 발생 조건, 호출 순서를 검증하라.
- DB 연동 동작 검증은 통합 테스트에 맡겨라.

---

## 통합 테스트 규칙

### UseCase 전용 호출 원칙

- 통합 테스트에서 **Service/Repository를 직접 호출하지 마라.**
- 데이터 셋업(given)을 포함한 모든 동작은 **UseCase 또는 Fixture(UseCase 조합)**를 통해 수행하라.
- UseCase가 자체 `@Transactional`을 관리하므로, 테스트에 `@Transactional`을 선언하지 마라.
- `@Transactional`을 테스트에 선언하면 실제 트랜잭션 경계가 무시되어 커밋 시점 검증이 불가능하다.

### Fixture 설계 규칙

- 테스트 전제조건(given) 셋업은 **Fixture 클래스**에 캡슐화하라.
- Fixture는 내부적으로 UseCase를 조합하여 데이터를 생성하라.
- Fixture는 인증/권한/테넌트 컨텍스트 설정을 담당하라.
- Fixture 메서드는 테스트에서 필요한 엔티티(ID 등)를 반환하라.
- Fixture 클래스는 `src/integrationTest/**/fixture/` 패키지에 위치시켜라.

**Fixture 메서드 네이밍 패턴:**
- 엔티티 생성: `createXxx()` — 예: `createPartWithReleasedRevision()`
- 흐름 실행: `executeXxxFlow()` — 예: `executeEcReleaseFlow()`
- 컨텍스트 설정: `setAuth()`, `setWorkflowMode()`

### 테스트 구조 규칙

- 테스트 클래스는 `PostgresIntegrationTestSupport`를 상속하라.
- `@BeforeEach`에서 Fixture를 통해 사용자 생성 및 인증 컨텍스트를 설정하라.
- 테스트 메서드는 **given-when-then** 구조를 따르되, 주석으로 구분하라.

### 검증(then) 규칙

- 검증은 **DB에서 다시 조회**하여 실제 영속된 상태를 확인하라.
- UseCase 반환값만으로 검증하지 마라 — 트랜잭션 커밋 후 DB 상태가 진짜다.
- 연관 엔티티의 상태도 함께 검증하라 (예: EC 릴리즈 시 리비전 RELEASED + 이전 리비전 SUPERSEDED).

### 시나리오 설계 규칙

- 하나의 테스트 클래스는 하나의 비즈니스 도메인 흐름을 다루라.
- **정상 흐름(happy path)**과 **예외 흐름(exception path)**을 모두 포함하라.
- 예외 흐름 테스트에서는 `assertThrows(AppException.class, ...)`로 검증하라.

---

## 공통 규칙

- 테스트 메서드명은 한글로 동작/시나리오를 설명하라.
- `@Test` 어노테이션을 빠뜨리지 마라.

## 빠른 체크리스트

### 도메인 단위 테스트
- 순수 Java인가? (Spring 의존 없음)
- 에러 코드까지 검증하는가?

### Service 단위 테스트
- `@ExtendWith(MockitoExtension.class)` 사용하는가?
- Mock으로 의존성을 격리하는가?

### 통합 테스트
- `PostgresIntegrationTestSupport`를 상속하는가?
- `@Transactional`이 없는가?
- Service/Repository를 직접 호출하지 않는가?
- given 셋업이 Fixture로 캡슐화되어 있는가?
- 검증이 DB 재조회로 이루어지는가?
- 정상 + 예외 흐름이 모두 포함되어 있는가?
