---
name: logging
description: |
  코드 작성 시 로깅이 필요한 위치와 레벨을 판단할 때 참조.
  OTel이 자동 처리하는 것과 로그로 남겨야 할 것을 구분한다.
disable-model-invocation: false
user-invocable: false
---

# OTel-Native 로깅 전략

> Trace = "어디로 갔는가", Log = "무슨 생각을 했는가"

## OTel vs Log 역할 분리

| 항목               | OTel (자동)   | Log (수동)          |
| ------------------ | ------------- | ------------------- |
| 수행 시간          | Span Duration | ❌ 로깅 금지        |
| 진입/퇴장          | Span 시작/끝  | ❌ 로깅 금지        |
| HTTP 요청/응답     | 자동 계측     | ❌ 로깅 금지        |
| 외부 API 호출      | 자동 계측     | ❌ 로깅 금지        |
| 비즈니스 분기 이유 | 모름          | ✅ 반드시 기록      |
| 데이터 예외/스킵   | 모름          | ✅ 반드시 기록      |
| 에러의 구체적 원인 | "실패"만 앎   | ✅ "왜 실패"를 기록 |

## 로그 레벨

| Level     | 용도                     | 예시                 |
| --------- | ------------------------ | -------------------- |
| `DEBUG`   | 개발 전용 (프로덕션 OFF) | 상세 데이터 덤프     |
| `INFO`    | 정상 비즈니스 이벤트     | 상태 변경, 정책 적용 |
| `WARNING` | 예상된 예외 상황         | 재시도, 폴백, 스킵   |
| `ERROR`   | 처리 실패 (복구 불가)    | 외부 서비스 오류     |

## 레이어별 가이드

### API Layer (`api/`)

- **로깅 최소화** — HTTP 요청/응답은 OTel이 처리
- 보안 위반(rate limit, 인증 실패)만 `WARNING`으로 기록

### Service Layer (`modules/*/service.py`)

- **비즈니스 분기점** 기록 — "왜 이 경로를 선택했는지"
- 예: 매핑 실패 이유, 인제스션 스킵 사유, 폴백 로직 진입

### Infrastructure Layer (`infrastructure/`)

- **에러의 구체적 맥락** 기록 — OTel은 "실패"만, 로그는 "왜 실패"
- 예: API 에러 코드, retry-after 값, 파싱 실패 원인

## 작성 규칙

1. **구조화된 extra 사용** — f-string 금지

   ```python
   # ❌ logger.info(f"매핑 실패: {header}")
   # ✅ logger.info("매핑 실패", extra={"header": header, "reason": reason})
   ```

2. **민감 정보 제외** — 비밀번호, 토큰, API 키 절대 포함 금지

3. **Trace ID 자동 연결** — OTel Span Context가 로그에 주입되어야 함 (설정은 `core/observability.py`)
