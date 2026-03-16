# Subscription Domain

## 가격 정책

Fabbit은 `플랜 + 좌석` 구조로 운영한다.

플랜:

- `Starter`
- `Team`
- `Org`
- `Enterprise`

좌석:

- `Viewer`
- `Collaborator`
- `Full Seat`

### 플랜별 가격 초안

- `Starter`
  - 무료
  - 최대 5명
  - 총 250MB
  - 월 100 AI 크레딧
- `Team`
  - `Viewer` 5,000원 / 인 / 월
  - `Collaborator` 15,000원 / 인 / 월
  - `Full Seat` 29,000원 / 인 / 월
  - 기본 10GB
  - `Full Seat`당 +10GB
- `Org`
  - `Viewer` 5,000원 / 인 / 월
  - `Collaborator` 25,000원 / 인 / 월
  - `Full Seat` 59,000원 / 인 / 월
  - 기본 100GB
  - `Full Seat`당 +50GB
- `Enterprise`
  - 별도 문의

### 스토리지 정책

- `Starter`는 총 250MB로 제한한다.
- `Team`, `Org`는 기본 제공량을 크게 준다.
- 추가 스토리지는 기본 제공량 초과분에 대해 `1GB당 200원 / 월`로 과금한다.
- 초과 스토리지는 워크스페이스 단위로 계산한다.
- `Unlimited`는 약속하지 않는다.

### AI 크레딧 정책

- `Starter`만 월 100 AI 크레딧을 기본 제공한다.
- `Viewer`는 AI 크레딧이 없다.
- `Team`, `Org`의 AI는 사용량 기반으로 과금한다.
- AI 사용량은 워크스페이스 단위로 합산한다.
- 관리자 설정에서 월간 AI 사용 한도와 초과 차단 여부를 설정할 수 있다.
- 한도 초과 시 AI 기능은 자동으로 차단한다.

---

## 좌석별 기능

### Viewer

- 조회
- 검색
- 부품/BOM/도면 열람
- 기본 다운로드

### Collaborator

- `Viewer` 기능 포함
- 코멘트
- 이슈 등록/응답
- 검토/승인
- 변경 요청

### Full Seat

- `Collaborator` 기능 포함
- 부품 생성/수정
- BOM 생성/수정
- 도면 업로드/수정
- 매핑
- 합성
- 관리자 작업

---

## 관리자 권한

- 관리자 권한은 좌석과 분리한다.
- `Viewer`, `Collaborator`, `Full Seat` 모두 조직 관리자 권한을 가질 수 있다.
- 단, 데이터 생성/수정 권한은 `Full Seat`에 한정한다.
