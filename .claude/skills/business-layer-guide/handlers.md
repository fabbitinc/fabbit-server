# modules/*/handlers.py 작성 규칙

## 역할

- 이벤트 반응 부수효과 — 도메인 경계를 넘는 상태 변경

## 트랜잭션

- 없음 — 이벤트 발행자와 같은 트랜잭션에서 실행

## 등록

- `app/core/event_registry.py`에 명시적 import

## 핸들러 유형

| 유형 | Session | 예외 처리 | 예시 |
|------|---------|-----------|------|
| 같은 트랜잭션 변경 | `get_active_session()` | 전파 (롤백) | file/handlers |
| 로그만 | 불필요 | 전파 (안전) | drawing, synthesis handlers |
| 별도 트랜잭션 | 자체 `SessionLocal()` | 자체 try/except | ai_usage (public 스키마) |

## 규칙

- service를 호출하지 않음 — 자기 도메인 모델/repo 직접 사용
- 같은 트랜잭션 내 변경이 필요하면 `get_active_session()`으로 현재 세션 획득
- 별도 트랜잭션이 필요한 경우(public 스키마 등)만 자체 `SessionLocal()` 사용
