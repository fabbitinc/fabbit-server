# TODOS

## 범용 번호 발번 시스템
- **What:** Part 전용 채번 시스템(`PartNumberCategory`, `PartNumberSequence`)을 Drawing, Issue 등 다른 도메인에도 재사용 가능하도록 일반화
- **Why:** 도면 번호, 이슈 번호 등에도 동일한 채번 로직이 필요할 수 있음. 현재 Part 전용으로 구현한 것을 범용 인프라로 전환하면 코드 중복 방지
- **Pros:** 도메인 간 일관된 번호 체계, 코드 재사용
- **Cons:** 과도 설계 위험, 실제 필요 시점까지 불필요한 추상화
- **Context:** 2026-03-29 CEO 리뷰에서 Part 전용으로 먼저 구현하기로 결정. 향후 다른 도메인에서 채번이 필요해지면 `number_sequences` (도메인타입 + 카테고리 기반)로 일반화
- **Effort:** M (human) -> S (CC+gstack)
- **Priority:** P3
- **Depends on:** Phase 1 채번 시스템 구현 완료
