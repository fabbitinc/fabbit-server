# CLAUDE.md

## 최우선 규칙

- 아래에 정의된 상황에서는 해당 스킬을 반드시 사용한다.
- 스킬 적용이 가능한데 사용하지 않는 행동을 금지한다.
- 하나의 요청이 여러 레이어를 포함하면 관련 스킬을 모두 적용한다.
- 답변/코드 변경 전, 먼저 대상 레이어를 식별하고 스킬을 로드한다.
- 변경사항에 대한 데이터베이스 마이그레이션은 사용자의 별도 요청이 없다면 고려하지 않는다.

## 강제 스킬 사용 규칙

### 1) Controller 작업

- 트리거:
  - Controller 추가/수정
  - API 엔드포인트 네이밍 변경
  - Request/Response DTO 네이밍/구조 변경
  - Swagger(OpenAPI) 어노테이션 작성/수정
- 반드시 사용할 스킬:
  - `controller-conventions`

### 2) EventHandler 작업

- 트리거:
  - 이벤트 소비 로직 추가/수정
  - `*EventHandler`, `*Event` 네이밍/구조 변경
  - 이벤트 재처리/멱등성 처리 규칙 논의
- 반드시 사용할 스킬:
  - `eventhandler-conventions`

### 3) UseCase 작업

- 트리거:
  - 유스케이스 추가/수정
  - `*Command/*Result` 경계 설계/변경
  - `execute(...)` 시그니처 변경
  - UseCase 트랜잭션 규칙 적용
- 반드시 사용할 스킬:
  - `usecase-conventions`

### 4) Query 작업

- 트리거:
  - 조회 전용 클래스 추가/수정
  - `*Condition/*Result` 모델 설계/변경
  - `list/get/search/lookup` 메서드 설계/변경
  - Query 클래스 트랜잭션(read-only) 규칙 적용
- 반드시 사용할 스킬:
  - `query-conventions`

### 5) Service 작업

- 트리거:
  - 서비스 클래스 추가/수정
  - 서비스 메서드 계약(입출력/네이밍) 변경
  - 서비스 책임 분리/통합 논의
- 반드시 사용할 스킬:
  - `service-conventions`

### 6) Repository 작업

- 트리거:
  - 저장소 인터페이스/메서드 추가/수정
  - `findBy/existsBy/countBy/deleteBy` 계약 변경
  - 엔티티 반환 규칙(`Optional/List`) 변경
  - Query와 Repository 책임 경계 조정
- 반드시 사용할 스킬:
  - `repository-conventions`

### 7) Test 작업

- 트리거:
  - `src/test/` 또는 `src/integrationTest/` 디렉터리의 테스트 추가/수정
  - 테스트 계층 선택 (단위 vs 통합) 논의
  - 통합 테스트 Fixture 설계
  - 테스트 구조/패턴 논의
- 반드시 사용할 스킬:
  - `test-conventions`

## 다중 레이어 요청 처리 규칙

- 요청이 두 개 이상 레이어를 포함하면, 해당 스킬을 모두 적용한다.
- 적용 순서는 요청의 변경 시작점 기준으로 선택하되, 누락 없이 전부 반영한다.
- 리뷰/질문/설계 토론도 동일하게 스킬 규칙을 강제 적용한다.

## 예외 처리

- 스킬 파일이 없거나 읽을 수 없으면 즉시 그 사실을 보고하고, 가능한 범위에서 가장 가까운 규칙을 적용한다.
- 예외 상황에서도 “스킬 사용 시도” 자체는 생략하지 않는다.

## 코드 변경 Hooks

- 코드 변경 작업을 수행한 경우
  - `make lint`를 할상 실행하여 규칙을 검사합니다.
  - `./gradlew test`를 항상 실행하여 전체 테스트 코드를 검사합니다.

## Skill routing

When the user's request matches an available skill, ALWAYS invoke it using the Skill
tool as your FIRST action. Do NOT answer directly, do NOT use other tools first.
The skill has specialized workflows that produce better results than ad-hoc answers.

Key routing rules:
- Product ideas, "is this worth building", brainstorming → invoke office-hours
- Bugs, errors, "why is this broken", 500 errors → invoke investigate
- Ship, deploy, push, create PR → invoke ship
- QA, test the site, find bugs → invoke qa
- Code review, check my diff → invoke review
- Update docs after shipping → invoke document-release
- Weekly retro → invoke retro
- Design system, brand → invoke design-consultation
- Visual audit, design polish → invoke design-review
- Architecture review → invoke plan-eng-review