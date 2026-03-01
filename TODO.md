- [ ] "기존 테넌트에는 upgrade로 데이터 변환 + 신규 테넌트에는 초기값 세팅이 필요" 할때 마이그레이션 전략 필요
- [ ] part, part_revision에 status 없음 <- 승인 관리 워크플로우가 있어야함 (draft, released 등) (lifecycle_state 는 제품 수명 주기)
- [ ] test2 폴더 구조에 맞게 테스트 옮겨야함
- [ ] 알림 구현
  1. 1단계: 백엔드에서 멘션 시 notification 레코드 생성 + 읽지 않은 알림 API
  2. 2단계: SSE 스트림 연결 (실시간 수신)
  3. 3단계: Chrome Notification API 연동
  4. ~~(추후): Service Worker + Web Push (오프라인 알림)~~
