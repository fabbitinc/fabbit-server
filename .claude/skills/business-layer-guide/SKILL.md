---
name: business-layer-guide
description: "비즈니스 레이어 아키텍처 규칙. queries, use_cases, service, handlers, 이벤트, 도메인 모델 코드를 작성하거나 수정할 때 자동 참조. 호출 흐름, 트랜잭션 경계, 이벤트 시스템, 레이어 간 의존성 규칙을 제공."
user-invocable: false
---

# 비즈니스 레이어 아키텍처

## 호출 흐름

```
router → queries/      (읽기) → repo
router → use_cases/    (쓰기) → service → repo
                                   ↓
                            model.register_event()
                                   ↓
                         UoW: collect → publish → commit
                                          ↓
                                    handlers/
```

## 레이어 요약

| 레이어 | 위치 | 역할 | 트랜잭션 |
|--------|------|------|----------|
| queries | `app/queries/{domain}/` | 읽기 전용 조회 | `@transactional(read_only=True)` |
| use_cases | `app/use_cases/{domain}/` | 쓰기 오케스트레이션 | `@transactional` |
| service | `app/modules/*/service.py` | 쓰기 비즈니스 로직 | 없음 (use_case가 관리) |
| handlers | `app/modules/*/handlers.py` | 이벤트 반응 부수효과 | 없음 (발행자와 같은 트랜잭션) |

## 이벤트 시스템

모든 이벤트는 **in-transaction** — commit 전 동기 발행.

```
model.register_event(Event)  →  UoW 수집  →  EventBus.publish()  →  db.commit()
```

- 핸들러 예외는 기본적으로 전파 → 트랜잭션 롤백
- 실패해도 롤백하면 안 되는 핸들러 → 핸들러 내부에서 자체 try/except
- 핸들러 등록: `app/core/event_registry.py`에 명시적 import

## 도메인 모델 규칙

- 자기 필드만 변경 — 다른 모델 필드를 직접 조작하지 않음
- 상태 전이는 의도가 드러나는 메서드로 제공: `mark_*`, `complete_*`, `fail_*`
- 다른 도메인 모델의 상태를 바꿔야 하면 → 이벤트 발행, 해당 도메인 handler가 처리

```
✓ part.detach_file()  → register_event(FileDetached)  → file handler가 file.mark_deleted()
✗ part.detach_file()  → file.mark_deleted()  (도메인 경계 침범)
```

## 의존성 규칙

```
✓ query     → repo
✓ use_case  → service (여러 도메인 조합 가능)
✓ service   → 자기 도메인 repo, infrastructure, 도메인 모델 메서드
✓ handler   → get_active_session() + 자기 도메인 모델/repo

✗ use_case  → infrastructure, repo, 도메인 모델 메서드 직접 호출
✗ query     → service
✗ service   → 타 도메인 service
✗ service   → use_case
✗ handler   → service
```

## 레이어별 상세 규칙

- queries 규칙: [queries.md](queries.md)
- use_cases 규칙: [use-cases.md](use-cases.md)
- service 규칙: [service.md](service.md)
- handlers 규칙: [handlers.md](handlers.md)
