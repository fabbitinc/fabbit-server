# LLM 챗 기능 설계

## 1. 목표

- 사용자가 자연어로 부품/이슈/변경관리 관련 질의를 수행한다.
- 단순 질의와 도메인 액션을 같은 대화 경험 안에서 처리한다.
- 쓰기 액션은 즉시 실행하지 않고 반드시 사용자 확인 단계를 거친다.
- 사용자에게는 내부 CoT 원문이 아닌 `가시화된 추론 로그`만 보여준다.

지원 시나리오 예시:

- 단순 채팅
- 품번 찾아줘
- 연결된 부품들 뭐야
- 어떤 부품에 대한 이슈 생성해줘

## 2. 설계 원칙

### 2.1 읽기와 쓰기 분리

- 읽기 액션: 바로 실행 가능
  - 예: 품번 검색, 부품 상세 조회, BOM 부모/자식 조회, 관련 이슈 조회
- 쓰기 액션: `초안 생성 -> 사용자 확인 -> 실제 실행`
  - 예: 이슈 생성, 변경관리 생성, 연결 관계 수정

### 2.2 CoT 직접 노출 금지

사용자에게 보여줄 것은 내부 사고 원문이 아니라 다음과 같은 이벤트다.

- `질문 의도를 파악했습니다`
- `품번 후보 3건을 찾았습니다`
- `BOM 부모/자식 관계를 조회했습니다`
- `이슈 초안을 생성했습니다. 확인 후 실행할 수 있습니다`

### 2.3 Chat 도메인을 독립 도메인으로 추가

- `chat`은 orchestration 도메인이다.
- 다른 도메인의 Repository를 직접 참조하지 않는다.
- 다른 도메인 접근은 `PartApi`, `IssueApi`, `EngineeringChangeApi` 같은 공개 경계를 통해 수행한다.

## 3. 핵심 아키텍처

```text
Client
  -> POST /api/v1/chat/threads/{threadId}/messages
  -> GET  /api/v1/chat/runs/{runId}/stream  (SSE)

ChatController
  -> SendChatMessageUseCase
    -> ChatService
    -> ChatAgentService
      -> ChatToolRegistry
        -> PartApi / IssueApi / EngineeringChangeApi / ...
      -> LlmChatPort

사용자 확인 필요 시
  -> ChatActionRequest 저장
  -> ConfirmChatActionUseCase
    -> 기존 CreateIssueUseCase 등 실행
```

## 4. 저장 모델

### 4.1 `chat_threads`

대화방 단위.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | UUID | PK |
| `org_id` | UUID | 조직 |
| `user_id` | UUID | 최초 생성 사용자 |
| `project_id` | UUID nullable | 프로젝트 문맥 |
| `context_type` | varchar(30) | `GLOBAL`, `PROJECT`, `PART`, `ISSUE`, `ENGINEERING_CHANGE` |
| `context_id` | UUID nullable | 문맥 대상 ID |
| `title` | varchar(200) | 자동 생성 제목 |
| `status` | varchar(20) | `ACTIVE`, `ARCHIVED` |
| `last_message_at` | timestamptz | 마지막 메시지 시각 |
| `created_at` | timestamptz | 생성 시각 |
| `updated_at` | timestamptz | 수정 시각 |

인덱스:

- `(org_id, user_id, created_at desc)`
- `(org_id, context_type, context_id)`
- `(org_id, last_message_at desc)`

### 4.2 `chat_messages`

대화 메시지 단위.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | UUID | PK |
| `thread_id` | UUID | FK |
| `run_id` | UUID nullable | assistant 생성 메시지면 run 연결 |
| `role` | varchar(20) | `USER`, `ASSISTANT`, `SYSTEM`, `TOOL` |
| `message_type` | varchar(20) | `TEXT`, `STRUCTURED`, `ERROR` |
| `content` | jsonb | 본문 |
| `status` | varchar(20) | `CREATED`, `STREAMING`, `COMPLETED`, `FAILED` |
| `sequence` | bigint | 스레드 내 정렬 |
| `created_at` | timestamptz | 생성 시각 |

`content` 예시:

```json
{
  "text": "품번 후보 3건을 찾았습니다.",
  "blocks": [
    {
      "type": "text",
      "text": "품번 후보 3건을 찾았습니다."
    },
    {
      "type": "part_lookup_result",
      "items": []
    }
  ]
}
```

인덱스:

- `(thread_id, sequence asc)`
- `(run_id)`

### 4.3 `chat_runs`

assistant 1회 실행 단위.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | UUID | PK |
| `thread_id` | UUID | FK |
| `user_message_id` | UUID | 입력 메시지 |
| `assistant_message_id` | UUID nullable | 출력 메시지 |
| `model` | varchar(100) | 모델명 |
| `intent` | varchar(50) | `GENERAL_CHAT`, `PART_LOOKUP`, `PART_BOM`, `ISSUE_CREATE_DRAFT` 등 |
| `status` | varchar(20) | `QUEUED`, `RUNNING`, `WAITING_CONFIRMATION`, `COMPLETED`, `FAILED`, `CANCELLED` |
| `input_tokens` | int | 입력 토큰 |
| `output_tokens` | int | 출력 토큰 |
| `error_code` | varchar(100) nullable | 실패 코드 |
| `metadata` | jsonb | 추가 메타데이터 |
| `started_at` | timestamptz nullable | 실행 시작 |
| `completed_at` | timestamptz nullable | 실행 종료 |
| `created_at` | timestamptz | 생성 시각 |

인덱스:

- `(thread_id, created_at desc)`
- `(status, created_at asc)`

### 4.4 `chat_run_events`

SSE 재생과 디버깅용 사용자 노출 이벤트 저장.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | UUID | PK |
| `run_id` | UUID | FK |
| `sequence` | bigint | 실행 내 순서 |
| `event_type` | varchar(50) | SSE 이벤트 타입 |
| `visibility` | varchar(20) | `USER_VISIBLE`, `INTERNAL` |
| `payload` | jsonb | 이벤트 데이터 |
| `created_at` | timestamptz | 생성 시각 |

주의:

- `INTERNAL`이라도 raw CoT는 저장하지 않는다.
- `payload`에는 사용자에게 보여줘도 되는 수준의 로그만 저장한다.

인덱스:

- `(run_id, sequence asc)`

### 4.5 `chat_action_requests`

승인 필요한 쓰기 액션.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | UUID | PK |
| `run_id` | UUID | FK |
| `thread_id` | UUID | FK |
| `action_type` | varchar(50) | `CREATE_ISSUE`, `CREATE_ENGINEERING_CHANGE` |
| `status` | varchar(20) | `PENDING`, `CONFIRMED`, `REJECTED`, `EXECUTED`, `FAILED`, `EXPIRED` |
| `preview_payload` | jsonb | 사용자 확인용 초안 |
| `request_payload` | jsonb | 실제 실행 입력 |
| `result_payload` | jsonb | 실행 결과 |
| `confirmed_by` | UUID nullable | 승인 사용자 |
| `confirmed_at` | timestamptz nullable | 승인 시각 |
| `executed_at` | timestamptz nullable | 실행 시각 |
| `expires_at` | timestamptz nullable | 만료 시각 |
| `created_at` | timestamptz | 생성 시각 |

인덱스:

- `(thread_id, created_at desc)`
- `(status, expires_at asc)`

## 5. 메시지와 이벤트 표현

### 5.1 사용자 메시지

```json
{
  "text": "MOTOR-001 품번 찾아주고 연결된 부품도 보여줘"
}
```

### 5.2 assistant 메시지

assistant 본문은 최종 답변 중심으로 저장한다.

```json
{
  "text": "MOTOR-001로 보이는 후보 2건을 찾았습니다. 첫 번째 품목 기준 부모 1건, 자식 4건이 연결되어 있습니다.",
  "blocks": [
    {
      "type": "text",
      "text": "MOTOR-001로 보이는 후보 2건을 찾았습니다."
    },
    {
      "type": "part_lookup_result",
      "items": []
    },
    {
      "type": "part_bom_result",
      "parents": [],
      "children": []
    }
  ]
}
```

### 5.3 visible trace 이벤트

```json
{
  "message": "품번 후보를 조회하는 중입니다",
  "step": "part_lookup",
  "status": "IN_PROGRESS"
}
```

## 6. SSE 통신 계약

### 6.1 추천 방식

- 메시지 생성: `POST`
- 응답 스트리밍: `SSE`
- 이유:
  - 현재 서버에 `StreamingResponseBody` 기반 스트림 패턴이 이미 존재한다.
  - 토큰/이벤트 순차 전달에 적합하다.
  - 프록시/재연결/keepalive 처리 부담이 WebSocket보다 작다.

### 6.2 API 초안

#### `POST /api/v1/chat/threads`

새 스레드 생성.

요청:

```json
{
  "contextType": "PROJECT",
  "contextId": "uuid",
  "title": "프로젝트 부품 질의"
}
```

응답:

```json
{
  "threadId": "uuid"
}
```

#### `GET /api/v1/chat/threads`

스레드 목록 조회.

#### `GET /api/v1/chat/threads/{threadId}`

스레드 상세 조회.

#### `GET /api/v1/chat/threads/{threadId}/messages`

메시지 목록 조회.

#### `POST /api/v1/chat/threads/{threadId}/messages`

사용자 메시지 전송 및 run 생성.

요청:

```json
{
  "text": "A-1000 품번 찾아줘"
}
```

응답:

```json
{
  "runId": "uuid",
  "messageId": "uuid",
  "status": "QUEUED"
}
```

#### `GET /api/v1/chat/runs/{runId}/stream`

SSE 스트림 연결.

헤더:

- `Content-Type: text/event-stream`
- `Cache-Control: no-cache`
- `X-Accel-Buffering: no`

#### `POST /api/v1/chat/action-requests/{actionRequestId}/confirm`

사용자 확인 후 실제 액션 실행.

#### `POST /api/v1/chat/action-requests/{actionRequestId}/reject`

쓰기 초안 거절.

### 6.3 SSE 이벤트 타입

#### `run.started`

```json
{
  "runId": "uuid",
  "status": "RUNNING"
}
```

#### `trace.updated`

```json
{
  "sequence": 1,
  "message": "질문 의도를 분석했습니다",
  "step": "intent_detection",
  "status": "COMPLETED"
}
```

#### `tool.started`

```json
{
  "toolName": "part_lookup",
  "displayName": "품번 검색",
  "input": {
    "keyword": "A-1000"
  }
}
```

#### `tool.completed`

```json
{
  "toolName": "part_lookup",
  "displayName": "품번 검색",
  "summary": "후보 2건을 찾았습니다"
}
```

#### `message.delta`

```json
{
  "textDelta": "후보 2건을 "
}
```

#### `action.required`

```json
{
  "actionRequestId": "uuid",
  "actionType": "CREATE_ISSUE",
  "preview": {
    "title": "A-1000 부품 간섭 이슈",
    "partIds": ["uuid"],
    "bodySummary": "간섭 가능성 점검 필요"
  }
}
```

#### `message.completed`

```json
{
  "messageId": "uuid"
}
```

#### `run.completed`

```json
{
  "runId": "uuid",
  "status": "COMPLETED"
}
```

#### `run.failed`

```json
{
  "runId": "uuid",
  "status": "FAILED",
  "errorCode": "CHAT_TOOL_EXECUTION_FAILED",
  "message": "부품 정보를 조회하지 못했습니다"
}
```

## 7. 서버 패키지 구조

### 7.1 Presentation

`src/main/java/com/fabbitinc/server/presentation/chat`

- `controller/ChatController.java`
- `dto/request/CreateChatThreadRequest.java`
- `dto/request/SendChatMessageRequest.java`
- `dto/request/ConfirmChatActionRequest.java`
- `dto/response/ChatThreadResponse.java`
- `dto/response/ChatThreadListResponse.java`
- `dto/response/ChatMessageListResponse.java`
- `dto/response/SendChatMessageResponse.java`

### 7.2 Application UseCase

`src/main/java/com/fabbitinc/server/application/chat/usecase`

- `CreateChatThreadUseCase`
- `SendChatMessageUseCase`
- `ConnectChatRunStreamUseCase`
- `ConfirmChatActionUseCase`
- `RejectChatActionUseCase`
- `CancelChatRunUseCase`

`command/result` 규칙:

- `CreateChatThreadCommand`, `CreateChatThreadResult`
- `SendChatMessageCommand`, `SendChatMessageResult`
- `ConfirmChatActionCommand`, `ConfirmChatActionResult`

### 7.3 Application Query

`src/main/java/com/fabbitinc/server/application/chat/query`

- `ChatQuery`
- `condition/ChatThreadListCondition`
- `condition/ChatThreadDetailCondition`
- `condition/ChatMessageListCondition`
- `result/ChatThreadListResult`
- `result/ChatThreadDetailResult`
- `result/ChatMessageListResult`

### 7.4 Application Service

`src/main/java/com/fabbitinc/server/application/chat/service`

- `ChatService`
  - thread/message/run 상태 관리
- `ChatAgentService`
  - intent 분류, tool 실행 orchestration, 응답 생성
- `ChatActionService`
  - 승인 필요한 액션 초안 생성/실행
- `ChatUsageService`
  - `ai_usage_events` 기록

### 7.5 Application Support / Port

`src/main/java/com/fabbitinc/server/application/chat/support`

- `ChatPromptBuilder`
- `ChatVisibleTraceFormatter`
- `ChatToolRegistry`
- `ChatToolContextFactory`
- `ChatMessageComposer`

`src/main/java/com/fabbitinc/server/application/chat/port`

- `LlmChatPort`
- `LlmChatStreamChunk`
- `LlmChatResponse`

### 7.6 Domain

`src/main/java/com/fabbitinc/server/domain/chat/model`

- `ChatThread`
- `ChatMessage`
- `ChatRun`
- `ChatRunEvent`
- `ChatActionRequest`

`src/main/java/com/fabbitinc/server/domain/chat/repository`

- `ChatThreadRepository`
- `ChatMessageRepository`
- `ChatRunRepository`
- `ChatRunEventRepository`
- `ChatActionRequestRepository`

## 8. 도메인 툴 설계

LLM이 직접 SQL이나 Repository를 만지지 않는다. `ChatToolRegistry`에 등록된 제한된 도구만 사용한다.

### 8.1 1차 도구 목록

#### `part_lookup`

- 목적: 품번/품명 검색
- 구현: `PartApi.searchPartSnapshots(...)`
- 결과: 후보 부품 목록

#### `part_bom`

- 목적: 특정 리비전의 부모/자식 BOM 조회
- 구현: chat 전용 read api 추가 권장
- 반환: `parents`, `children`

권장 추가 API:

- `PartApi.getPartBomSnapshot(UUID partId, UUID revisionId)`

#### `issue_lookup`

- 목적: 특정 부품과 연결된 이슈 조회
- 구현: `IssueApi`에 chat 전용 read 메서드 추가 권장

권장 추가 API:

- `IssueApi.getIssueSnapshotsByPartIds(Set<UUID> partIds)`

#### `issue_create_draft`

- 목적: 이슈 생성 초안 작성
- 구현: 실제 생성하지 않고 `chat_action_requests` 저장
- 실제 실행은 `ConfirmChatActionUseCase` 에서 `CreateIssueUseCase` 호출

### 8.2 툴 사용 규칙

- 한 번의 run에서 쓰기 액션은 최대 1건만 생성한다.
- 툴 실패 시 assistant는 실패를 숨기지 않고 사용자에게 설명한다.
- 동일 질문에서 후보가 여러 개인 경우 즉시 쓰기 액션을 만들지 않는다.
- 식별 대상이 모호하면 먼저 disambiguation 질문을 반환한다.

## 9. 실행 흐름

### 9.1 단순 조회

예: `A-1000 품번 찾아줘`

1. 사용자 메시지 저장
2. `chat_run` 생성
3. SSE 연결
4. intent = `PART_LOOKUP`
5. `part_lookup` 실행
6. visible trace 이벤트 발행
7. assistant 메시지 스트리밍
8. run 완료
9. usage 기록

### 9.2 복합 조회

예: `A-1000 연결된 부품들 뭐야`

1. 품번 검색
2. 후보 1건이면 바로 BOM 조회
3. 후보 여러 건이면 assistant가 선택 질문 반환
4. 결과를 structured block으로 응답

### 9.3 쓰기 액션

예: `이 부품 이슈 생성해줘`

1. 대상 부품 식별
2. 제목/본문/부품 연결 정보 초안 생성
3. `chat_action_request` 저장
4. `action.required` 이벤트 발행
5. 사용자가 confirm
6. `ConfirmChatActionUseCase` 에서 `CreateIssueUseCase.execute(...)`
7. 결과를 assistant 메시지로 추가

## 10. 상태 머신

### 10.1 `chat_runs.status`

- `QUEUED`
- `RUNNING`
- `WAITING_CONFIRMATION`
- `COMPLETED`
- `FAILED`
- `CANCELLED`

### 10.2 `chat_action_requests.status`

- `PENDING`
- `CONFIRMED`
- `REJECTED`
- `EXECUTED`
- `FAILED`
- `EXPIRED`

## 11. 권한과 안전장치

### 11.1 권한

- 스레드/메시지/런 조회는 동일 org 범위와 사용자 접근 문맥을 검증한다.
- 프로젝트 문맥 스레드는 해당 프로젝트 접근 권한이 있어야 한다.
- 쓰기 액션 confirm 시에도 다시 권한 검증한다.

### 11.2 안전장치

- raw CoT 저장/노출 금지
- PII/민감정보는 trace payload에 넣지 않음
- assistant가 작성한 액션 초안은 무조건 사용자 confirm 필요
- 툴 입력은 서버가 검증/정규화
- timeout/재시도 정책을 `LlmChatPort` 에서 통제

## 12. 토큰/비용 기록

기존 `ai_usage_events` 재사용.

권장 값:

- `category`: `CHAT`
- `feature`: `chat_thread_message`
- `model`: 실제 응답 모델
- `metadata`:

```json
{
  "threadId": "uuid",
  "runId": "uuid",
  "intent": "PART_LOOKUP",
  "toolNames": ["part_lookup", "part_bom"]
}
```

## 13. 구현 우선순위

### 1단계

- thread/message/run/action_request 테이블
- `ChatController`
- `SendChatMessageUseCase`
- `ConnectChatRunStreamUseCase`
- `part_lookup`, `part_bom`
- visible trace + final message 스트리밍

### 2단계

- `issue_create_draft`
- confirm/reject 플로우
- `CreateIssueUseCase` 연결

### 3단계

- 프로젝트/변경관리 문맥
- 추천 질문
- 대화 요약 및 thread title 자동 개선

## 14. 권장 구현 메모

- 스트리밍은 현재 알림 스트림과 동일하게 `StreamingResponseBody + BlockingQueue` 패턴을 우선 사용한다.
- Query 레이어 규칙상 chat 조회는 `ChatQuery` 에 모은다.
- UseCase는 `@Transactional`, Query는 `@Transactional(readOnly = true)`를 유지한다.
- chat이 다른 도메인을 직접 조회해야 할 때는 해당 도메인의 `*Api`를 추가해서 경계를 맞춘다.

## 15. 결론

이 기능은 `대화 UI`보다 `승인 가능한 도메인 에이전트`로 설계해야 한다.

정리하면 다음 구조가 가장 안전하다.

- 저장: `chat_threads`, `chat_messages`, `chat_runs`, `chat_run_events`, `chat_action_requests`
- 통신: `POST + SSE`
- 노출: raw 생각과정 대신 visible trace
- 실행: 읽기 액션은 즉시, 쓰기 액션은 초안 후 confirm
- 구조: `chat` 독립 도메인 + 기존 `Part/Issue/EngineeringChange` 공개 API 재사용
