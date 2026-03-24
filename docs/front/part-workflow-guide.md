# Part 워크플로 UI 가이드

## 개요

Part(부품)에는 두 가지 독립적인 상태 축이 있다:
- **PartRevisionStatus** — 리비전(버전)의 문서 상태
- **PartLifecycleState** — 부품 자체의 사업적 수명 상태

두 상태 모두 **워크플로 모드**에 따라 전환 방식이 달라진다.

---

## 워크플로 모드

| 모드 | 의미 |
|------|------|
| `DIRECT` | 사용자가 UI에서 직접 리비전 릴리즈/취소, lifecycle 전환 가능 |
| `ENGINEERING_CHANGE_REQUIRED` | EC(변경관리)를 통해서만 리비전 릴리즈, lifecycle 전환 가능 |

### 모드 조회

```
GET /api/v1/settings
→ { "partWorkflowMode": "DIRECT" }
```

### 모드 변경

```
PUT /api/v1/settings/parts/workflow-policy
{ "mode": "ENGINEERING_CHANGE_REQUIRED" }
```

> 진행 중인 EC가 있으면 모드 변경이 차단된다.

---

## 1. Part Revision 상태 전환

### 상태 다이어그램

```
DRAFT ──release──→ RELEASED ──(자동)──→ SUPERSEDED
  │
  └──cancel──→ CANCELED
```

- **DRAFT**: 작성 중인 초안
- **RELEASED**: 현행 유효 버전
- **SUPERSEDED**: 후속 리비전이 릴리즈되어 대체된 이전 버전
- **CANCELED**: 릴리즈 전에 폐기된 초안

### DIRECT 모드 — UI 흐름

```
Part 상세 화면
  └─ DRAFT 리비전 선택
       ├─ [릴리즈] 버튼 → POST /{partId}/revisions/{revisionId}/release
       │    body: { "reason": "변경 사유" }  ← 필수
       │    → 이전 RELEASED 리비전은 자동으로 SUPERSEDED
       │
       ├─ [취소] 버튼 → POST /{partId}/revisions/{revisionId}/cancel
       │    body: { "reason": "취소 사유" }  ← 필수
       │
       └─ [새 초안] 버튼 → POST /{partId}/revisions/{revisionId}/draft
            body: { "reason": "생성 사유" }
            → RELEASED 또는 SUPERSEDED 리비전에서만 생성 가능
            → Part가 OBSOLETE이면 차단됨
```

### EC 모드 — UI 흐름

```
Part 상세 화면
  └─ DRAFT 리비전 선택
       ├─ [릴리즈] 버튼 → 비활성화 (회색 처리)
       │    툴팁: "변경관리를 통해 릴리즈해야 합니다"
       │
       ├─ [취소] 버튼 → 비활성화
       │    툴팁: "변경관리를 통해 처리해야 합니다"
       │
       └─ [새 초안] 버튼 → 활성 (초안 생성은 모드 무관)

EC 상세 화면
  └─ 영향 항목 섹션
       └─ [리비전 추가] → DRAFT 리비전 선택 picker
            itemType: "REVISION_RELEASE"
            targetId: revisionId
```

---

## 2. Part Lifecycle 상태 전환

### 상태 다이어그램

```
ACTIVE ──→ EOL ──→ OBSOLETE
  │                    ↑
  └────────────────────┘
```

- **ACTIVE**: 활성 — 신규 설계, 조달, 생산 모두 가능
- **EOL** (End of Life): 단종 예정 — 기존 유지보수만, 신규 설계 사용 금지
- **OBSOLETE**: 폐기 — 더 이상 사용 불가, 새 초안 생성 차단

> OBSOLETE는 되돌릴 수 없다. 전환 전 확인 다이얼로그 필수.

### DIRECT 모드 — UI 흐름

```
Part 상세 화면
  └─ Lifecycle 상태 표시 (배지 또는 라벨)
       └─ [상태 변경] 드롭다운 또는 버튼
            ├─ ACTIVE 상태일 때: [EOL로 전환] [OBSOLETE로 전환]
            ├─ EOL 상태일 때: [OBSOLETE로 전환]
            └─ OBSOLETE 상태일 때: 전환 버튼 없음

API:
  POST /api/v1/parts/{partId}/lifecycle
  { "targetState": "EOL" }
  → { "partId": "...", "lifecycleState": "EOL" }
```

**OBSOLETE 전환 시 확인 다이얼로그:**
```
"이 부품을 폐기(OBSOLETE) 처리하시겠습니까?
 폐기된 부품은 되돌릴 수 없으며, 새 초안을 생성할 수 없습니다."
[취소] [폐기 처리]
```

### EC 모드 — UI 흐름

```
Part 상세 화면
  └─ Lifecycle 상태 표시
       └─ [상태 변경] 버튼 → 비활성화
            툴팁: "변경관리를 통해 상태를 변경해야 합니다"

EC 상세 화면
  └─ 영향 항목 섹션
       └─ [Lifecycle 변경 추가] → Part 선택 picker + 대상 상태 선택
            itemType: "LIFECYCLE_CHANGE"
            targetId: partId
            targetState: "EOL" 또는 "OBSOLETE"
```

---

## 3. EC 영향 항목 (Affected Items) UI

EC 상세 화면의 "영향 항목" 섹션은 두 가지 타입을 지원한다.

### 영향 항목 목록 표시

```
┌──────────────────────────────────────────────────────┐
│ 영향 항목                                    [+ 추가] │
├──────────────────────────────────────────────────────┤
│ 📄 REVISION_RELEASE  AES-001 Rev B (DRAFT)     [삭제] │
│ 📄 REVISION_RELEASE  AES-002 Rev A (DRAFT)     [삭제] │
│ 🔄 LIFECYCLE_CHANGE  AES-003  ACTIVE → EOL     [삭제] │
│ 🔄 LIFECYCLE_CHANGE  AES-004  ACTIVE → OBSOLETE[삭제] │
└──────────────────────────────────────────────────────┘
```

### 영향 항목 추가 UI

```
[+ 추가] 클릭 시 타입 선택:
  ├─ [리비전 릴리즈] → DRAFT 리비전 picker
  │    → itemType: "REVISION_RELEASE", targetId: revisionId
  │
  └─ [Lifecycle 변경] → Part picker + 상태 선택
       → itemType: "LIFECYCLE_CHANGE", targetId: partId, targetState: "EOL"
```

### 동기화 API

```
PUT /api/v1/engineering-changes/{ecId}/affected-items
{
  "items": [
    { "itemType": "REVISION_RELEASE", "targetId": "<revisionId>", "targetState": null },
    { "itemType": "LIFECYCLE_CHANGE", "targetId": "<partId>", "targetState": "EOL" }
  ]
}
```

### 영향 항목 응답 구조

```json
{
  "id": "affected-item-uuid",
  "itemType": "REVISION_RELEASE",
  "targetId": "revision-or-part-uuid",
  "actionDetail": null,
  "partId": "part-uuid",
  "partNumber": "AES-001",
  "revisionCode": null,
  "name": "메인 하우징",
  "status": "DRAFT"
}
```

> `LIFECYCLE_CHANGE` 항목의 경우 `partId`, `partNumber`는 null일 수 있다. `actionDetail`에 `{"targetState":"EOL","previousState":"ACTIVE"}` JSON이 들어간다.

---

## 4. EC 승인 흐름

```
DRAFT ──submit──→ REVIEW_PENDING ──review approve──→ APPROVAL_PENDING
                       ↑                                    │
                       └──────reject──────────────←─────────┤
                                                            │
                                                      ──approve──→ RELEASE_PENDING
                                                                        │
                       ┌──────reject──────────────←─────────────────────┤
                       ↓                                                │
                     DRAFT                                        ──release──→ RELEASED
                                                                     (영향 항목 일괄 반영)

어느 단계에서든:
  ──cancel──→ CANCELED (영향 항목 중 리비전 DRAFT는 CANCELED, lifecycle 변경은 롤백)
```

### EC Release 시 자동 처리

| 영향 항목 타입 | Release 시 동작 |
|---|---|
| `REVISION_RELEASE` | 해당 DRAFT 리비전 → RELEASED, 이전 리비전 → SUPERSEDED |
| `LIFECYCLE_CHANGE` | 해당 Part의 lifecycleState 전환 |

### EC Cancel 시 자동 처리

| 영향 항목 타입 | Cancel 시 동작 |
|---|---|
| `REVISION_RELEASE` | DRAFT 상태인 리비전 → CANCELED |
| `LIFECYCLE_CHANGE` | Part의 lifecycleState를 이전 상태로 원복 |

---

## 5. 모드별 버튼 활성화 요약

| 동작 | DIRECT 모드 | EC 모드 |
|------|-------------|---------|
| 리비전 릴리즈 | ✅ 활성 | ❌ 비활성 (EC 통해 처리) |
| 리비전 취소 | ✅ 활성 | ❌ 비활성 (EC 통해 처리) |
| 새 초안 생성 | ✅ 활성 | ✅ 활성 |
| Lifecycle 전환 | ✅ 활성 | ❌ 비활성 (EC 통해 처리) |
| EC 생성 | ➖ (불필요) | ✅ 활성 |

---

## 6. 제약 조건 (프론트 에러 처리)

| 조건 | API 응답 | UI 처리 |
|------|---------|---------|
| EC 모드에서 직접 릴리즈 시도 | `PART_WORKFLOW_POLICY_FORBIDDEN` | 버튼 비활성화로 사전 차단 |
| OBSOLETE Part에서 새 초안 생성 | `PART_OBSOLETE` | 버튼 비활성화 + 툴팁 |
| 잘못된 lifecycle 전환 (OBSOLETE→ACTIVE) | `PART_LIFECYCLE_TRANSITION_INVALID` | 허용된 전환만 드롭다운에 표시 |
| 진행 중 EC가 있을 때 모드 변경 | `CONFLICT` | 에러 메시지 표시 |
