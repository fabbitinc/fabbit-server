# Query 변경 필요 목록

- 기준: `query-conventions` 스킬의 `Condition/Result`, read-only, 조회 전용 책임 분리
- 상태 표기: `pending`, `in_progress`, `done`

## 진행 순서

- `done` `part`: `PartQuery`, `PartOwnerQuery`
- `done` `project`: `ProjectQuery`
- `done` `member`: `MemberQuery`
- `done` `team`: `TeamQuery`
- `done` `issue`: `IssueQuery`
- `done` `mapping`: `MappingQuery`
- `done` `label`: `LabelQuery`
- `done` `supplier`: `SupplierQuery`
- `done` `notification`: `NotificationQuery`
- `done` `organization`: `OrganizationInvitationQuery`
- `done` `auth`: `AuthQuery`, `AuthInvitationQuery`
- `done` `user`: `UserQuery`
- `done` `usage`: `UsageQuery`
- `done` `synthesis`: `SynthesisQuery`
- `done` `dashboard`: `DashboardQuery`
- `done` `ontology`: `OntologyQuery`
- `done` `activation`: `ActivationQuery`

## part 작업 항목

- `done` `PartQuery` 입력 파라미터를 `Condition`으로 정리
- `done` `PartQuery` 반환 타입을 `Result`로 분리
- `done` `PartOwnerQuery` 반환 타입을 `Result`로 분리
- `done` `PartController`, `PartOwnerController`는 `Result -> Response` 매핑만 담당하도록 정리
- `done` `PartQuery`에서 엔티티 relation getter 직접 의존 제거
- `done` `PartOwnerQuery`에서 `UserApi`, `TeamApi`로 보조 조회 분리
- `done` 1-depth BOM 조회는 native query 대신 QueryDSL로 전환
- `done` 재귀 BOM tree 조회/export는 recursive CTE가 필요해 native query 예외 유지

## part 특이사항

- `GET /api/v1/parts/{partId}/projects` 응답은 `project` DTO 대신 `part` 도메인 Response로 정리
- export 계열도 입력은 `Condition`으로 통일하고, 반환은 파일 바이트를 유지

## 기존 규칙 준수로 종료

- `project`: 이미 `Condition/Result`, class-level `@Transactional(readOnly = true)`, controller `Result -> Response` 매핑 구조를 따름
- `member`: 이미 `Condition/Result`, class-level `@Transactional(readOnly = true)`, controller `Result -> Response` 매핑 구조를 따름
- `auth`: `AuthQuery`, `AuthInvitationQuery` 모두 `Condition/Result` 구조와 controller 매핑이 이미 적용돼 있음
- `user`: `UserQuery`는 `MeCondition/MeResult`와 controller 매핑 구조를 이미 따름
- `organization`: `OrganizationInvitationQuery`는 `Result` 반환과 controller 매핑 구조를 이미 따름

## 이번 라운드 완료 항목

- `team`: `TeamQuery`, `TeamController`, `TeamMemberController`를 `Condition/Result` + controller 매핑 구조로 정리
- `issue`: `IssueQuery`, `IssueController`, `ChangeRequestController`를 `Condition/Result` + controller 매핑 구조로 정리
- `mapping`: `MappingQuery`, `MappingController`를 `Condition/Result` + controller 매핑 구조로 정리
- `label`: `LabelQuery`, `LabelController`를 `Condition/Result` + controller 매핑 구조로 정리
- `supplier`: `SupplierQuery`, `SupplierController`를 `Condition/Result` + controller 매핑 구조로 정리
- `notification`: `NotificationQuery`, `NotificationController`를 `Condition/Result` + controller 매핑 구조로 정리
- `usage`: `UsageQuery`, `UsageController`를 `Condition/Result` + controller 매핑 구조로 정리
- `synthesis`: `SynthesisQuery`, `SynthesisController`를 `Condition/Result` + controller 매핑 구조로 정리
- `dashboard`: `DashboardQuery`, `DashboardController`를 `Condition/Result` + controller 매핑 구조로 정리
- `ontology`: `OntologyQuery`, `OntologyController`를 `Condition/Result` + controller 매핑 구조로 정리
- `activation`: `ActivationQuery`, `ActivationController`를 `Condition/Result` + controller 매핑 구조로 정리

## 특이사항

- query/controller 경계에서 재사용하던 enum은 response 패키지에서 분리했다.
- `ActivityAction`, `ActivityScope`는 `application.activity.model`로 이동했다.
- `BomDirection`은 `application.part.model`로 이동했다.
- `StorageCategory`, `StorageTrendPeriod`는 `application.usage.model`로 이동했다.
- controller는 model enum을 직접 파라미터로 받지 않고 raw query param을 `Condition`에 전달한 뒤 Query 내부에서 해석하도록 정리했다.
- 재귀 CTE가 필요한 BOM tree 조회/export만 native query 예외를 유지한다.
