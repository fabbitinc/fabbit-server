---
name: test-guide
description: "테스트 작성 규칙. test 파일, fixture, mock, 테스트 케이스를 작성하거나 수정할 때 자동 참조. 테스트 레이어 구분, 네이밍, 폴더 구조, marker 규칙을 제공."
user-invocable: false
---

# 테스트 작성 규칙

## 테스트 레이어

### 1. unit

- 목적: 함수/메서드 단위 로직·분기 검증. 외부 의존성은 전부 mock.
- 케이스 수: 성공/실패/엣지 최소 3개 + 분기마다 1개 추가.
- 외부 클라이언트 장애 대응은 여기서 적극 검증:
  - timeout, 5xx, 잘못된 응답 포맷, 재시도 실패, fallback/circuit-breaker, 예외 매핑(도메인 에러 변환).
- DB: 실제 DB 미사용, Repository 인터페이스 mock 사용.

### 2. e2e core (TestClient)

- 목적: 모든 API 엔드포인트의 동작 보장(회귀 방어).
- 외부 연동: mock/fake 사용. MinIO처럼 로컬 컨테이너로 안정적으로 띄울 수 있으면 실제 연동 가능.
- 포커스: 플로우/계약(상태코드, 주요 응답 필드, 핵심 부작용).
- 원칙: 세부 에러 분기 대부분은 unit에서 검증. e2e는 "연결된 시스템으로서 정상 작동" 확인.

### 3. e2e external (TestClient)

- 목적: e2e core에서 mock했던 외부 연동 지점을 실제 호출하여 건강성 검증(샌드박스/테스트 키).
- 범위: LLM/결제/메일 등 외부 API 의존 엔드포인트 중심.
- 운영: PR 필수 아님, 수동/야간/릴리즈 전 실행.

### 4. llm_eval

- 목적: 모델/프롬프트 품질·비용·지연 평가.
- 지표: 정답 일치율, 구조 유효성, 지연, 비용.
- 운영: 게이트 제외, 리포트 축적.

### 5. smoke_httpx (httpx + BASE_URL)

- 목적: 네트워크 경계 검증 전용.
- 범위: 소수 핵심 시나리오(5~10개)만.
- 운영: 릴리즈 전/장애 재현 시.

## e2e 전용 오류 테스트

아래는 unit으로 커버할 수 없어 e2e에서 반드시 확인해야 하는 항목:

- FastAPI 요청 검증/직렬화 오류 (422/response schema)
- 인증/권한 미들웨어 체인 (401/403)
- DI/라우팅/테넌트 컨텍스트 wiring 오류
- 트랜잭션 경계에서의 실제 롤백/커밋 관찰
- 멱등성(중복 요청) 같은 API 레벨 행위

## httpx 필수 사용 케이스

TestClient가 아닌 httpx를 반드시 쓰는 경우:

1. 실제 서버 프로세스/포트/워커/timeout 영향 확인이 필요할 때
2. CORS preflight, 프록시 헤더, 스트리밍/대용량 업로드 등 네트워크 경계 이슈 확인
3. TestClient로 재현 안 되는 운영 이슈 재현
4. 배포 직전 실서버 URL 스모크 확인

## 폴더 구조

```
test2/
├── unit/...
├── e2e/
│   ├── core/...
│   └── external/...
├── llm_eval/...
└── smoke_httpx/...
```

## Marker / CI

| marker        | 대상 레이어              | 비고            |
| ------------- | ------------------------ | --------------- |
| `unit`        | unit                     |                 |
| `e2e`         | e2e core + external 공통 |                 |
| `external`    | e2e external 전용        | e2e와 함께 부착 |
| `llm_eval`    | llm_eval                 |                 |
| `smoke_httpx` | smoke_httpx              |                 |
| `costly`      | 비용 발생 테스트         | 레이어 무관     |

- `pytest -m "e2e and not external"` → core만
- `pytest -m e2e` → core + external 전체
- `pytest -m e2e and external` → external만

| 실행 시점 | 대상                      |
| --------- | ------------------------- |
| PR 필수   | unit + e2e (not external) |
| 야간/수동 | e2e external + llm_eval   |
| 릴리즈 전 | smoke_httpx 필수 1회      |

## 데이터 격리

- 테스트별 고유 tenant(suffix 또는 전용 스키마)를 사용한다. 테스트 간 데이터 공유 금지.
- 생성한 데이터는 테스트 전용 네임스페이스에 격리하고, teardown에서 정리한다.

## 작성 규칙

### 네이밍

- unit: `test_<대상>_success`, `test_<대상>_failure_<원인>`, `test_<대상>_edge_<조건>`
- e2e: "흐름 시나리오 파일"로 관리, 내부 분기 에러는 unit으로 내린다.

### 테스트 분배 원칙

- 외부 API 장애 케이스는 unit에서 촘촘히, e2e external은 "연결 확인" 중심으로 얇게.
- 세부 에러 분기는 unit, e2e는 플로우 중심.
