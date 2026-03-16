# Part Domain

## 목적

Part 도메인은 제조업 관점에서 아래 3개 축을 분리해서 다룬다.

- `Part`: 품번 마스터의 운영 수명
- `PartRevision`: 특정 시점의 기술 정의본
- `EngineeringChange`: 기술 정의 변경을 검토, 승인, 반영하는 워크플로

핵심 원칙은 다음과 같다.

- 모든 정의 정보는 `PartRevision` 자산이다
- 변경 프로세스는 `EngineeringChange`가 담당한다
- `PartRevision`에는 워크플로 상태를 넣지 않는다
- `DIRECT` 모드에서는 `EngineeringChange` 없이 바로 반영한다

---

## Aggregate 역할

### Part

`Part`는 품번 자체를 의미한다.

책임:

- 품번 식별
- 품번 마스터 운영 상태 관리
- 현재 공식 리비전 포인터 관리

`Part`는 기술 정의를 직접 갖지 않는다. 기술 정의는 모두 `PartRevision`에 속한다.

### PartRevision

`PartRevision`은 특정 시점의 기술 정의본이다.

포함 자산 예시:

- 속성
- 도면
- 첨부
- EBOM 스냅샷
- 대표 미리보기 상태

`PartRevision`은 "무엇이 정의되었는가"를 표현하고, `EngineeringChange`는 "그 정의를 어떤 절차로 반영하는가"를 표현한다.

### EngineeringChange

`EngineeringChange`는 draft revision들을 묶어서 검토, 승인, 반영하는 워크플로다.

책임:

- 변경 대상 revision 묶기
- 검토자, 승인자, 반영자 지정
- 현재 워크플로 단계 표현
- 반려, 승인, 반영, 폐기 이력 관리

---

## 상태 모델

### Part.lifecycleState

`Part`는 품번 자체의 운영 수명만 표현한다.

- `ACTIVE`
- `EOL`
- `OBSOLETE`

의미:

- `ACTIVE`: 신규 설계, 구매, 생산에 사용 가능
- `EOL`: 신규 적용은 줄이되 기존 운영은 가능
- `OBSOLETE`: 더 이상 사용하지 않음

이 값은 revision workflow와 자동 연동하지 않는다.

### PartRevision.status

`PartRevision`은 기술 정의본의 상태만 표현한다.

- `DRAFT`
- `RELEASED`
- `SUPERSEDED`
- `CANCELED`

의미:

- `DRAFT`: 아직 공식 반영 전인 작업본
- `RELEASED`: 현재 또는 과거의 공식 정의본
- `SUPERSEDED`: 더 최신 `RELEASED`에 의해 대체된 과거 공식본
- `CANCELED`: 폐기된 작업본

중요:

- `IN_REVIEW`, `APPROVED`는 `PartRevision` 상태가 아니다
- 검토, 승인, 반영 대기는 `EngineeringChange`가 표현한다

### EngineeringChange.status

`EngineeringChange`는 변경 프로세스 상태를 표현한다.

- `DRAFT`
- `REVIEW_PENDING`
- `APPROVAL_PENDING`
- `RELEASE_PENDING`
- `RELEASED`
- `CANCELED`

의미:

- `DRAFT`: 변경안 작성 중
- `REVIEW_PENDING`: 검토자 검토 대기
- `APPROVAL_PENDING`: 승인자 승인 대기
- `RELEASE_PENDING`: 반영자 배포 대기
- `RELEASED`: 변경 반영 완료
- `CANCELED`: 변경안 폐기

주의:

- 예전의 포괄적 제출/머지 상태 대신 현재 차례가 보이도록 상태를 분리한다
- `RELEASED`는 실제 release 완료를 의미한다

---

## 역할 모델

`EngineeringChange`는 `EngineeringChangeStep` 공통 엔티티로 workflow 담당자를 관리한다.

- `stepType = REVIEW`
- `stepType = APPROVAL`
- `stepType = RELEASE`
- `assigneeType = USER | TEAM`

### reviewer

검토 단계 담당자다.

- 개인 사용자 또는 팀 단위로 연결 가능
- `EngineeringChangeStep(stepType=REVIEW)`로 표현
- 현재 활성 검토 step만 처리 가능
- 검토 단계에서 `반려` 가능

### approver

최종 승인 담당자다.

- `EngineeringChangeStep(stepType=APPROVAL)`로 표현
- 개인 사용자 또는 팀 단위로 연결 가능
- 여러 step을 순차 또는 병렬(sequence 기준)로 구성 가능
- 검토가 끝난 변경안을 승인 가능

### releaser

반영 담당자다.

- `EngineeringChangeStep(stepType=RELEASE)`로 표현
- 개인 사용자 또는 팀 단위로 연결 가능
- 여러 step을 순차 또는 병렬(sequence 기준)로 구성 가능
- 승인 완료된 변경안을 실제 반영 가능

### 역할별 권한

- `반려`: reviewer만 가능
- `승인`: approver만 가능
- `반영`: releaser만 가능

---

## 상태 전이

### DIRECT 모드

`DIRECT` 모드에서는 `EngineeringChange`를 사용하지 않는다.

전이:

- `PartRevision.DRAFT -> RELEASED`
- `PartRevision.DRAFT -> CANCELED`
- 새 `RELEASED`가 생기면 이전 `RELEASED -> SUPERSEDED`

의미:

- draft 작성 후 바로 공식 반영
- 승인, 검토, 배포 대기 단계 없음

### EngineeringChange 모드

`EngineeringChange` 모드에서는 draft revision을 `EngineeringChange`에 연결해서 진행한다.

기본 전이:

- `EngineeringChange.DRAFT -> REVIEW_PENDING`
- `EngineeringChange.REVIEW_PENDING -> DRAFT`
- `EngineeringChange.REVIEW_PENDING -> APPROVAL_PENDING`
- `EngineeringChange.APPROVAL_PENDING -> DRAFT`
- `EngineeringChange.APPROVAL_PENDING -> RELEASE_PENDING`
- `EngineeringChange.RELEASE_PENDING -> DRAFT`
- `EngineeringChange.RELEASE_PENDING -> RELEASED`
- `EngineeringChange.DRAFT -> CANCELED`
- `EngineeringChange.REVIEW_PENDING -> CANCELED`
- `EngineeringChange.APPROVAL_PENDING -> CANCELED`
- `EngineeringChange.RELEASE_PENDING -> CANCELED`

반영 시 revision 전이:

- 연결된 `PartRevision.DRAFT -> RELEASED`
- 기존 `Part.currentReleasedRevisionId`가 가리키던 revision은 `SUPERSEDED`

폐기 시 revision 전이:

- 아직 반영되지 않은 연결 revision은 `CANCELED`

---

## 반려, 폐기, 보관

사용자 액션 용어는 아래처럼 구분한다.

- `반려`
- `폐기`
- `보관`

### 반려

의미:

- 수정 후 다시 진행해야 함

결과:

- `EngineeringChange`는 `DRAFT`로 돌아감
- 연결된 `PartRevision`은 그대로 `DRAFT` 상태 유지
- 기존 내용은 롤백하지 않음
- 사용자는 같은 draft를 계속 수정한다

즉 반려는 "처음부터 다시 작성"이 아니라 "다시 수정 가능한 상태로 복귀"다.

### 폐기

의미:

- 이 변경 시도 자체를 종료함

결과:

- `EngineeringChange -> CANCELED`
- 아직 반영되지 않은 연결 revision은 `CANCELED`

`CANCELED`된 revision은 다시 열지 않는다. 다시 진행하려면 새 draft를 만든다.

### 보관

의미:

- 목록 정리용 보조 기능

결과:

- 상태 전이 없음
- revision 상태도 바꾸지 않음

보관은 workflow 핵심 상태가 아니라 UI 관리 기능이다.

---

## CANCELED 정책

### PartRevision.CANCELED

의미:

- 특정 revision 시도를 폐기함

정책:

- 수정 불가
- 재요청 불가
- 승인, 반영 불가
- 조회만 가능

다시 진행하려면:

- 기존 `CANCELED` revision을 되살리지 않는다
- 기준 revision에서 새 `DRAFT`를 생성한다

### EngineeringChange.CANCELED

의미:

- 이 변경안 전체를 더 이상 진행하지 않음

정책:

- workflow 종료
- 미반영 revision도 함께 종료 대상이 된다

### Part.OBSOLETE와의 관계

`PartRevision.CANCELED`와 `Part.OBSOLETE`는 같은 개념이 아니다.

- `Part.OBSOLETE`: 품번 자체 종료
- `PartRevision.CANCELED`: 특정 변경 시도 종료

둘을 자동 동기화하지 않는다.

---

## Current Pointer 규칙

`Part`는 현재 공식 revision을 가리키는 포인터를 가진다.

- `currentReleasedRevisionId`

권장 규칙:

- 항상 현재 공식본은 하나만 유지
- 새 revision이 반영되면 이전 공식본은 `SUPERSEDED`

---

## 모드 변경 정책

워크플로 모드는 진행 중 건이 없을 때만 변경한다.

금지 조건:

- 진행 중 revision 존재
  - `PartRevision.DRAFT`
- 진행 중 `EngineeringChange` 존재
  - `DRAFT`
  - `REVIEW_PENDING`
  - `APPROVAL_PENDING`
  - `RELEASE_PENDING`

정책:

- 기존 건에 새 모드를 소급 적용하지 않는다
- 진행 중 건을 먼저 종료한 뒤 모드를 바꾼다

---

## UI 해석

### DIRECT

- draft 상세에서 `반영`, `취소`

### EngineeringChange

- draft 상세는 작성, 조회 중심
- 상태 전이는 `EngineeringChange` 화면에서 수행

예시 액션:

- `검토 요청`
- `반려`
- `승인`
- `반영`
- `폐기`
- 필요 시 `보관`

권장 표현:

- workflow 상태는 `EngineeringChange.status` 기준으로 노출
- revision은 정의본 상태만 보여준다
- 사용자가 현재 누구 차례인지 바로 알 수 있어야 한다

---

## 설계 원칙 요약

- `Part`는 품번 수명을 표현한다
- `PartRevision`은 기술 정의본 상태를 표현한다
- `EngineeringChange`는 검토, 승인, 반영 workflow를 표현한다
- workflow 상태와 revision 상태를 섞지 않는다
- `DIRECT`와 `EngineeringChange` 모드는 서로 다른 흐름으로 본다
- `CANCELED`는 닫힌 상태이며 다시 열지 않는다
