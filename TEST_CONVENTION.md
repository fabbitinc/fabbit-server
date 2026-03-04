1. unit

- 기준: 함수/메서드 단위 로직 검증, 외부 의존성은 전부 mock.
- 기본 케이스: 성공/실패/엣지 최소 3개.
- 더 좋은 기준: 분기 기준으로 작성. 분기 2개면 3개가 아니라 분기마다 최소 1개씩 추가.
- 외부 클라이언트 장애 대응 검증: 여기서 적극 검증.
- 예: timeout, 5xx, 잘못된 응답 포맷, 재시도 실패, fallback/circuit-breaker, 예외 매핑(도메인 에러 변환).
- DB: unit에서는 실제 DB 미사용, Repository 인터페이스 mock 사용.

2. e2e-core (TestClient)

- 기준: 모든 API 엔드포인트의 동작 보장(회귀 방어).
- 외부 연동: mock/fake 사용.
- 예외: MinIO처럼 로컬 컨테이너로 안정적으로 띄울 수 있으면 실제 연동 가능.
- 포커스: 플로우/계약(상태코드, 주요 응답 필드, 핵심 부작용).
- 원칙: 세부 에러 분기 대부분은 unit에서 검증하고, e2e는 “연결된 시스템으로서 정상 작동” 확인.

3. e2e-external (TestClient)

- 기준: e2e-core에서 mock했던 외부 연동 지점을 실제 호출.
- 범위: LLM/결제/메일 등 외부 API 의존 엔드포인트 중심.
- 목적: 실연동 건강성 검증(샌드박스/테스트 키).
- 운영: PR 필수 아님, 수동/야간/릴리즈 전 실행.

4. llm-eval

- 기준: 테스트라기보다 모델/프롬프트 성능 검증.
- 지표: 정답 일치율, 구조 유효성, 지연, 비용.
- 운영: 게이트 제외, 리포트 축적 중심.

5. “e2e에서만 가능한 오류 테스트 있나?”

- 있음. 아래는 e2e에서 최소한 확인해야 함.
- FastAPI 요청 검증/직렬화 오류(422/response schema).
- 인증/권한 미들웨어 체인(401/403).
- DI/라우팅/테넌트 컨텍스트 wiring 오류.
- 트랜잭션 경계에서의 실제 롤백/커밋 관찰.
- 멱등성(중복 요청) 같은 API 레벨 행위.

6. CI 권장

- PR 필수: unit + e2e-core.
- 야간/수동: e2e-external + llm-eval.
- 릴리즈 전: e2e-external 스모크 필수 1회.

7. 작성 규칙(간단 고정)

- unit 네이밍: test*<대상>**success, **failure*<원인>, \__edge_<조건>.
- e2e는 “흐름 시나리오 파일”로 관리, 내부 분기 에러는 unit으로 내린다.
- 외부 API 장애 케이스는 unit에서 촘촘히, e2e-external은 “연결 확인” 중심으로 얇게.

---

• 최종 운영안 (httpx 포함)

1. unit

- 목적: 로직/분기 검증.
- 기준: 최소 성공/실패/엣지 + 분기 추가 케이스.
- 외부 연동 장애(LLM/결제/메일/S3 timeout, 5xx, invalid response)는 여기서 mock으로 검증.
- DB는 mock/FakeSession 사용(실DB 금지).

2. e2e-core (TestClient)

- 목적: API 회귀 보장.
- 범위: 모든 API 엔드포인트 플로우.
- 외부 연동은 mock/fake, MinIO 등 로컬 인프라는 실제 사용 가능.
- 원칙: 플로우 중심, 세부 오류 분기는 unit 중심.

3. e2e-external (TestClient)

- 목적: e2e-core에서 mock한 외부 연동만 실제 호출 검증.
- 범위: LLM/결제/메일 등 실제 샌드박스 호출 케이스.
- 운영: 수동/야간/릴리즈 전.

4. llm-eval

- 목적: 모델/프롬프트 품질·비용·지연 평가.
- 운영: 게이트 제외, 리포트 축적.

5. httpx-smoke (httpx + BASE_URL)

- 목적: 네트워크 경계 검증 전용.
- 범위: 소수 핵심 시나리오(5~10개)만.
- 운영: 릴리즈 전/장애 재현 시.

httpx를 반드시 쓰는 케이스

1. 실제 서버 프로세스/포트/워커/timeout 영향 확인이 필요할 때.
2. CORS preflight, 프록시 헤더, 스트리밍/대용량 업로드 등 네트워크 경계 이슈를 확인할 때.
3. TestClient로 재현 안 되는 운영 이슈를 재현할 때.
4. 배포 직전 실서버 URL 스모크 확인이 필요할 때.

폴더 분리 권장

1. test2/unit/...
2. test2/e2e/core/...
3. test2/e2e/external/...
4. test2/llm_eval/...
5. test2/smoke_httpx/... ← 별도 폴더 권장

marker/CI

1. marker: unit, e2e, external, llm, eval, httpx, costly
2. PR 필수: unit + e2e-core
3. 야간/수동: e2e-external + llm-eval
4. 릴리즈 전: smoke_httpx 실행
