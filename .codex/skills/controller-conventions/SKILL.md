---
name: controller-conventions
description: Java Spring REST API의 Controller 레이어 네이밍, DTO, Swagger(OpenAPI) 작성 규칙을 일관되게 적용한다. 컨트롤러 추가/수정, 엔드포인트 메서드명 정리, Request/Response 타입 표준화, HTTP 상태코드와 API 문서 규칙 정리가 필요할 때 사용한다.
---

# Controller Conventions

## 목표

- Spring Controller의 엔드포인트/메서드/DTO 네이밍을 일관되게 유지하라.
- API 계약을 컨트롤러 경계에서 명확히 관리하라.
- 이 스킬은 Controller 규칙만 다루고 다른 레이어 규칙은 다루지 마라.

## 네이밍 규칙

- Controller 클래스: `*Controller`
- Request DTO: `*Request`
- Response DTO: `*Response`
- 목록 응답: `*ListResponse`
- 상세 응답: `*DetailResponse`
- 자동완성/팝오버 조회 응답: `*LookupResponse` (필요 시 `*LookupItemResponse`)
- 엔드포인트 메서드명: 동사 기반 (`create`, `list`, `get`, `update`, `delete`, `archive`)

## Swagger 규칙

- 모든 Controller 클래스에 `@Tag(name, description)`를 선언하라.
- 모든 엔드포인트 메서드에 `@Operation(summary, description)`를 선언하라.
- 모든 엔드포인트 메서드의 `@Operation`에 `operationId`를 명시하라.
- `operationId`는 OpenAPI 전체에서 유일한 lowerCamelCase로 작성하고, 리소스와 동작이 드러나게 적어라. 예: `partGet`, `partList`, `bomItemUpdate`
- `@Operation.summary`에는 HTTP 메서드나 URL 경로를 넣지 말고, API가 하는 행위를 짧게 적어라.
- `@Operation.description`에는 대상, 조건, 보조 설명을 적고 필요하면 경로 문맥을 풀어서 설명하라.
- 모든 엔드포인트에 `@ApiResponses`를 작성하라.
- 성공 응답 코드를 명시하라: `200`, `201`, `204`.
- 공통 실패 응답 코드를 명시하라: `400`, `401`, `403`, `404`.
- 주요 파라미터(`@PathVariable`, `@RequestParam`, `@RequestHeader`)에 `@Parameter`로 설명을 작성하라.
- Request/Response DTO 필드에 `@Schema(description, example)`를 작성하라.
- Bean Validation(`@NotBlank`, `@Size`, `@Min`, `@Max`)과 `@Schema`를 함께 사용하라.

## 도메인 의존 규칙

- Controller는 자기 도메인의 `UseCase/Query`만 참조하라.
- Controller에서 다른 도메인의 `UseCase/Query/Service/Repository`를 직접 참조하지 마라.

## 적용 절차

1. URL 경로와 HTTP 메서드를 먼저 확정하라.
2. 입력 DTO를 `*Request`로, 출력 DTO를 `*Response`로 명명하라.
3. 단건/목록/자동완성 반환 여부에 따라 `*DetailResponse`, `*ListResponse`, `*LookupResponse`를 적용하라.
4. Swagger 어노테이션(`@Tag`, `@Operation`, `@ApiResponses`)을 채워 API 문서를 완성하라.
5. DTO 필드 문서(`@Schema`)와 입력 검증(Bean Validation)을 함께 작성하라.
6. 성공 응답 상태코드를 엔드포인트 의도와 일치시키라 (`200`, `201`, `204`).
7. 컨트롤러 시그니처가 API 계약만 표현하는지 점검하라.

## 빠른 체크리스트

- 클래스명이 `*Controller`로 끝나는가?
- 입력/출력 DTO 이름이 `*Request/*Response` 규칙을 따르는가?
- 목록/상세/자동완성 응답 이름이 용도별로 구분되는가?
- 모든 엔드포인트에 `@Operation`, 고유한 `operationId`, `@ApiResponses`가 있는가?
- Request/Response DTO 필드에 `@Schema`가 있는가?
- 생성/삭제 엔드포인트의 상태코드가 `201/204`로 일관적인가?
- 컨트롤러 메서드명이 HTTP 동작과 의미가 맞는가?
- Controller가 다른 도메인의 `UseCase/Query`를 직접 참조하지 않는가?
