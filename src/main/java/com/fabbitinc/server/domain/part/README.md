# Part Domain

## 구조

- `Part`
  - 품번 마스터
  - 운영 수명만 표현한다
- `PartRevision`
  - 특정 시점의 기술 정의본
  - 속성, 도면, 첨부, BOM, 대표 미리보기는 모두 여기에 속한다
- `EngineeringChange`
  - draft revision을 검토, 승인, 반영하는 workflow

중요 원칙:

- `PartRevision`은 기술 정의 상태만 가진다
- 검토, 승인, 반영 workflow 상태는 `EngineeringChange`가 가진다

## 상태

### Part.lifecycleState

- `ACTIVE`
- `EOL`
- `OBSOLETE`

의미:

- `ACTIVE`: 사용 가능
- `EOL`: 신규 적용 축소
- `OBSOLETE`: 품번 자체 종료

### PartRevision.status

- `DRAFT`
- `RELEASED`
- `SUPERSEDED`
- `CANCELED`

의미:

- `DRAFT`: 수정 가능한 작업본
- `RELEASED`: 공식 정의본
- `SUPERSEDED`: 더 최신 공식본에 의해 대체된 과거본
- `CANCELED`: 폐기된 작업본

### EngineeringChange.state

- `DRAFT`
- `REVIEW_PENDING`
- `APPROVAL_PENDING`
- `RELEASE_PENDING`
- `RELEASED`
- `CANCELED`

의미:

- `DRAFT`: 변경안 작성 중
- `REVIEW_PENDING`: 검토 단계
- `APPROVAL_PENDING`: 승인 단계
- `RELEASE_PENDING`: 반영 단계
- `RELEASED`: 변경 반영 완료
- `CANCELED`: 변경안 폐기

## 상태 전이

### DIRECT 모드

`EngineeringChange`를 사용하지 않는다.

전이:

- `PartRevision.DRAFT -> RELEASED`
- `PartRevision.DRAFT -> CANCELED`
- 새 `RELEASED`가 생기면 이전 `RELEASED -> SUPERSEDED`

### EngineeringChange 모드

`PartRevision`은 계속 `DRAFT`이고, workflow는 `EngineeringChange`가 표현한다.

전이:

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

반영 시:

- 연결된 `PartRevision.DRAFT -> RELEASED`
- 기존 `Part.currentReleasedRevisionId`가 가리키던 공식본은 `SUPERSEDED`

폐기 시:

- 아직 반영되지 않은 연결 `PartRevision`은 `CANCELED`

## 상태별 변경 가능 범위

### PartRevision.DRAFT

수정 가능 상태다.

개념적으로 수정 가능한 범위:

- 기본 속성
- 첨부 파일
- 도면
- 대표 미리보기
- BOM
- 기타 revision 정의 정보

주의:

- 현재 API surface는 완전히 정렬되지 않았다
- 도메인 원칙상 초기 draft와 revision 파생 draft의 수정 가능 범위는 같아야 한다

### PartRevision.RELEASED

공식본이다.

- 직접 수정 불가
- 조회 가능
- 새 변경이 필요하면 새 `DRAFT`를 만든다

### PartRevision.SUPERSEDED

과거 공식본이다.

- 직접 수정 불가
- 조회 가능

### PartRevision.CANCELED

폐기된 작업본이다.

- 수정 불가
- 재사용 불가
- 조회만 가능
- 다시 진행하려면 새 `DRAFT`를 만든다

### EngineeringChange.DRAFT

- 변경 대상 draft 연결 가능
- step 구성 가능
- 내용 수정 가능
- 제출 가능
- 폐기 가능

### EngineeringChange.REVIEW_PENDING

- reviewer만 검토 step 처리 가능
- 반려 가능
- 폐기 가능
- draft 내용은 계속 수정 가능하지만, workflow 상태는 `EngineeringChange` 화면에서 제어한다

### EngineeringChange.APPROVAL_PENDING

- approver만 승인 step 처리 가능
- 반려 가능
- 폐기 가능

### EngineeringChange.RELEASE_PENDING

- releaser만 반영 step 처리 가능
- 반려 가능
- 폐기 가능

### EngineeringChange.RELEASED

- 변경 반영 완료
- workflow 종료

### EngineeringChange.CANCELED

- workflow 종료
- 다시 열지 않는다

## 반려와 폐기

- `반려`
  - workflow를 다시 수정 가능한 상태로 되돌린다
  - `EngineeringChange -> DRAFT`
  - 연결된 `PartRevision`은 그대로 `DRAFT`
  - 내용은 롤백하지 않는다

- `폐기`
  - 이 변경 시도 자체를 종료한다
  - `EngineeringChange -> CANCELED`
  - 미반영 `PartRevision`은 `CANCELED`

## 모드 변경

workflow 모드는 진행 중 건이 없을 때만 바꾼다.

모드 변경 금지 조건:

- `PartRevision.DRAFT` 존재
- `EngineeringChange.DRAFT`
- `EngineeringChange.REVIEW_PENDING`
- `EngineeringChange.APPROVAL_PENDING`
- `EngineeringChange.RELEASE_PENDING`

## 현재 공식본

`Part`는 현재 공식 revision 포인터를 가진다.

- `currentReleasedRevisionId`

규칙:

- 항상 현재 공식본은 하나만 유지한다
- 새 revision이 반영되면 이전 공식본은 `SUPERSEDED` 된다
