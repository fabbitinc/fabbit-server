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
