# TODOS

## Review

### Part change history cursor pagination

**What:** `GET /api/v1/parts/{partId}/change-history`를 offset/limit에서 cursor pagination으로 전환 검토.

**Why:** 데모 범위에서는 충분하지만, 실제 사용자 화면에서 부품 이력이 수백 건 쌓이면 뒤 페이지로 갈수록 offset 기반 조회가 느려질 수 있다.

**Context:** 2026-03-29 `/plan-eng-review`에서 기록. 현재 구현은 `PartChangeHistoryQuery`에서 issue + EC release + revision history를 union하고 `OFFSET/FETCH`를 사용한다. 지금은 데모/초기 운영에는 충분하지만, 사용량이 붙으면 cursor 기반 정렬 키(예: timestamp + referenceId)로 옮기는 편이 안전하다.

**Effort:** M
**Priority:** P2
**Depends on:** 변경 이력 API 실제 사용량 확인

## 범용 번호 발번 시스템
- **What:** Part 전용 채번 시스템(`PartNumberCategory`, `PartNumberSequence`)을 Drawing, Issue 등 다른 도메인에도 재사용 가능하도록 일반화
- **Why:** 도면 번호, 이슈 번호 등에도 동일한 채번 로직이 필요할 수 있음. 현재 Part 전용으로 구현한 것을 범용 인프라로 전환하면 코드 중복 방지
- **Pros:** 도메인 간 일관된 번호 체계, 코드 재사용
- **Cons:** 과도 설계 위험, 실제 필요 시점까지 불필요한 추상화
- **Context:** 2026-03-29 CEO 리뷰에서 Part 전용으로 먼저 구현하기로 결정. 향후 다른 도메인에서 채번이 필요해지면 `number_sequences` (도메인타입 + 카테고리 기반)로 일반화
- **Effort:** M (human) -> S (CC+gstack)
- **Priority:** P3
- **Depends on:** Phase 1 채번 시스템 구현 완료

## Batch import 파일명 품번 검증 실패 전략
- **What:** Inventor 마이그레이션 임포트 시 파일명이 Part 품번 검증을 통과 못할 때 (한글, 공백, 특수문자 등) 단건 skip vs 전체 실패 정책 결정
- **Why:** Outside voice(CEO review)에서 발견. 미정의 시 대량 임포트에서 예측 불가능한 동작. Preview 단계에서 WARNING으로 표시하고 사용자가 수정 가능하도록 해야 할 수 있음
- **Pros:** 정책이 명확하면 사용자 신뢰 증가, 에러 메시지 구체화 가능
- **Cons:** 없음 (결정 비용만)
- **Context:** 2026-03-30 CEO 리뷰. IPJ 샘플 확보 후 실제 파일명 패턴을 보고 결정
- **Effort:** S (human) -> S (CC+gstack)
- **Priority:** P1
- **Depends on:** IPJ 샘플 확보

## SseManager 멀티 인스턴스 대응
- **What:** SseManager를 Redis Pub/Sub로 교체하여 다중 서버 인스턴스에서 SSE 이벤트 전달
- **Why:** 현재 단일 인스턴스 전제 (코드에 TODO 주석 존재). 마이그레이션 진행률 SSE가 다중 인스턴스에서 동작하지 않음
- **Pros:** 스케일 대응, 마이그레이션/알림 등 장시간 SSE 이벤트 안정성
- **Cons:** Redis 의존성 추가, 인프라 복잡도 증가
- **Context:** 2026-03-30 CEO 리뷰. Outside voice에서 SseManager 단일 인스턴스 전제 지적
- **Effort:** M (human) -> S (CC+gstack)
- **Priority:** P3
- **Depends on:** 다중 서버 인스턴스 운영 시작
