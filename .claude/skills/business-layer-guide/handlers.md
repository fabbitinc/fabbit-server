# modules/*/handlers.py 작성 규칙

## 역할

- 이벤트 반응 부수효과 — 도메인 경계를 넘는 상태 변경

## 트랜잭션

- 없음 — 이벤트 발행자와 같은 트랜잭션에서 실행

## 등록

새 핸들러 모듈 추가 시 2단계:

1. `app/modules/{domain}/handlers.py` 하단에서 `event_bus.subscribe()` 호출
2. `app/core/event_registry.py`의 `register_event_handlers()`에 해당 모듈 import 추가

```python
# 1. app/modules/{domain}/handlers.py 하단
event_bus.subscribe(SomeEvent, _on_some_event)

# 2. app/core/event_registry.py
def register_event_handlers() -> None:
    import app.modules.file.handlers  # noqa: F401
    import app.modules.{domain}.handlers  # noqa: F401  # ← 추가
```

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
