# Test Rules

## 목적

- 레이어별 테스트 방식, 모킹 범위, fixture 관리 규칙을 통일한다.
- 테스트 작성 시 무엇을 검증하고 무엇을 모킹할지 빠르게 결정할 수 있게 한다.

## 기본 원칙

- 테스트는 대상 레이어의 책임만 검증하라.
- 한 테스트 클래스는 한 대상 클래스만 검증하라.
- 테스트 이름은 의도를 드러내라 (`should_*` 권장).
- 성공 케이스와 실패 케이스를 최소 1개씩 포함하라.
- 랜덤값 기반 테스트를 지양하고 재현 가능한 데이터로 작성하라.

## 레이어별 테스트 전략

| 레이어       | 기본 테스트 타입                                 | 모킹 대상                              | 모킹 금지/최소화                       |
| ------------ | ------------------------------------------------ | -------------------------------------- | -------------------------------------- |
| Controller   | `@WebMvcTest` + `MockMvc`                        | `UseCase`, `Query`                     | `Service`, `Repository` 직접 모킹 금지 |
| EventHandler | 단위 테스트 (`JUnit` + `Mockito`)                | 호출 대상 `UseCase`                    | DB/웹 계층 모킹 불필요                 |
| UseCase      | 단위 테스트 (`JUnit` + `Mockito`)                | `Service`, 외부 Port                   | Controller DTO/웹 객체 모킹 금지       |
| Query        | DB 연동 테스트 (`@DataJpaTest` 또는 통합 테스트) | 인증/권한 파서 등 부수 의존성만 선택적 | Querydsl/JPA 조회 결과 자체 모킹 지양  |
| Service      | 단위 테스트 (`JUnit` + `Mockito`)                | `Repository`, 외부 Port                | 웹 계층 객체 모킹 금지                 |
| Repository   | DB 연동 테스트 (`@DataJpaTest`)                  | 없음                                   | Repository 자체 모킹 금지              |

## 모킹 규칙

- 한 단계 아래 의존성만 모킹하라.
- 반환값 중심으로 검증하고, 상호작용 검증(`verify`)은 핵심 호출만 사용하라.
- 과도한 interaction 검증 나열을 금지한다.
- `deep stub` 사용을 금지한다.
- Query/Repository 테스트에서는 가능하면 실제 DB 결과를 검증하라.

## Fixture 규칙

- 공통 fixture 위치: `src/test/java/.../support/fixture`
- fixture 클래스 네이밍: `*Fixture` (`UserFixture`, `ProjectFixture`)
- 기본 데이터 생성 메서드와 부분 변경 메서드를 함께 제공하라.
- 시간/UUID/식별자는 고정 가능한 값으로 관리하라.
- 전역 fixture는 최소화하고, 시나리오 특화 데이터는 테스트 클래스 내부에서 보완하라.

## 트랜잭션 및 DB 테스트 규칙

- Repository/Query DB 테스트는 롤백 가능한 방식으로 격리하라.
- 테스트 간 데이터 의존성을 만들지 마라.
- 정렬/페이징/커서/검색 조건은 경계값을 포함해 검증하라.

## 최소 케이스 기준

- Controller: 성공 1건 + 검증 실패 1건 + 인증/인가 실패 1건
- EventHandler: 정상 처리 1건 + 중복/예외 처리 1건
- UseCase: 정상 시나리오 1건 + 비즈니스 실패 1건
- Query: 필터/정렬/페이징(또는 커서) 1세트
- Service: 핵심 도메인 규칙 성공/실패 각 1건
- Repository: 커스텀 조회 조건 성공/경계값 각 1건

---

## 엔티티 테스트 최소 기준

- 생성 성공 1건
- 불변식 위반 1건
- 상태 전이 성공 1건
- 금지 전이(또는 중복 전이) 1건
