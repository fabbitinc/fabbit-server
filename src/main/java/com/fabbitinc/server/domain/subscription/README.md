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
  - `Collaborator`당 월 100 AI 크레딧 포함
  - `Full Seat`당 월 500 AI 크레딧 포함
- `Org`
  - `Viewer` 5,000원 / 인 / 월
  - `Collaborator` 25,000원 / 인 / 월
  - `Full Seat` 59,000원 / 인 / 월
  - 기본 100GB
  - `Full Seat`당 +50GB
  - `Collaborator`당 월 200 AI 크레딧 포함
  - `Full Seat`당 월 1,500 AI 크레딧 포함
- `Enterprise`
  - 별도 문의

### 스토리지 정책

- `Starter`는 총 250MB로 제한한다.
- `Team`, `Org`는 기본 제공량을 크게 준다.
- 추가 스토리지는 필요 시 별도 증설 가능하다.
- `Unlimited`는 약속하지 않는다.

### AI 크레딧 정책

- 기본 AI 크레딧은 좌석별로 포함한다.
- `Viewer`는 AI 크레딧이 없다.
- `Collaborator`, `Full Seat`는 플랜별 월 기본 AI 크레딧을 가진다.
- 조직 공용 AI 크레딧은 기본 제공하지 않는다.
- 조직 공용 AI 크레딧은 추가 구매로만 제공한다.
- 추가 구매한 조직 공용 AI 크레딧은 좌석 종류와 관계없이 조직 전체가 함께 사용한다.

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
