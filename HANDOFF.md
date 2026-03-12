# HANDOFF

## Goal

- `www`/base-domain 로그인에서 발급하는 `create_org` scoped token 조건을 정리하고, 이미 조직을 만든 사용자가 다시 조직 생성 플로우로 들어가지 않도록 막는다.
- 최근 auth/config 관련 변경사항을 새 컨텍스트의 에이전트가 바로 이어받을 수 있게 정리한다.

## Current Progress

- `AuthController`의 `login`은 `Origin`을 request body가 아니라 `@RequestHeader("Origin")`로 받는다는 점을 확인했다.
- `src/main/java/com/fabbitinc/server/application/auth/usecase/LoginUseCase.java`
  - `slug == null || slug.isBlank()` 분기에서 `organizationApi.hasOwnedOrganization(userId)`를 먼저 확인한다.
  - 이미 생성한 워크스페이스가 있으면 `FORBIDDEN`과 `"이미 생성한 워크스페이스가 있습니다. 해당 워크스페이스에서 로그인해주세요"` 메시지를 던진다.
- `src/main/java/com/fabbitinc/server/application/organization/service/OrganizationService.java`
  - `createOrganization(...)`에서 `hasOwnedOrganization(userId)`를 검사한다.
  - 이미 생성한 워크스페이스가 있으면 `ALREADY_EXISTS`와 `"이미 생성한 워크스페이스가 있어 새 조직을 생성할 수 없습니다"` 메시지를 던진다.
- `src/main/java/com/fabbitinc/server/application/organization/api/OrganizationApi.java`
  - `hasOwnedOrganization(UUID userId)` 메서드를 추가했다.
- `src/main/java/com/fabbitinc/server/domain/organization/repository/OrganizationRepository.java`
  - `existsByOwnerId(UUID ownerId)` 메서드를 추가했다.
- 테스트 추가/수정
  - `src/test/java/com/fabbitinc/server/application/auth/usecase/LoginUseCaseTest.java`
  - `src/test/java/com/fabbitinc/server/application/organization/service/OrganizationServicePersistenceTest.java`
- 설정 추가
  - `src/main/resources/application.properties`에 아래 3개를 명시했다.
  - `app.email-verification-expire-minutes=10`
  - `app.email-verification-max-attempts=5`
  - `app.email-verification-cooldown-seconds=10`

## What Worked

- “이미 조직을 만든 사용자”를 owner 기준으로 해석하기 위해 `Organization.ownerId`를 활용하는 접근이 맞았다.
- `OrganizationRepository.existsByOwnerId(...)`를 추가하니 로그인 차단과 실제 조직 생성 차단을 같은 기준으로 맞출 수 있었다.
- 아래 테스트 커맨드는 통과했다.
  - `./gradlew test --tests com.fabbitinc.server.application.auth.usecase.LoginUseCaseTest --tests com.fabbitinc.server.application.organization.service.OrganizationServicePersistenceTest`

## What Didn't Work

- 처음에는 “멤버십이 하나라도 있으면 차단”으로 구현했는데, 요구사항인 “조직을 이미 만든애”보다 범위가 넓어서 owner 기준으로 다시 좁혔다.
- 테스트 작성 중 `PlanType.PRO`를 사용했는데 실제 enum에 없어 컴파일 에러가 났다. `PlanType.BUSINESS`로 수정했다.

## Next Steps

- 제품 요구사항이 “owner만 차단”이 맞는지 확인한다.
- 만약 초대 멤버도 public-site에서 조직 생성이 불가능해야 한다면, 현재 owner 기준을 membership 기준으로 다시 넓혀야 한다.
- 프론트가 `FORBIDDEN` 메시지 `"이미 생성한 워크스페이스가 있습니다. 해당 워크스페이스에서 로그인해주세요"`를 어떻게 처리할지 확인한다.
- 필요하면 auth/organization 관련 더 넓은 회귀 테스트를 추가로 수행한다.
- `AppProperties`의 다른 `@DefaultValue`들도 `application.properties`에 명시할지 정리한다.
