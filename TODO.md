- [ ] 1. create_all + stamp와 데이터 마이그레이션  
     맞는 지적입니다. 현재 설계는 신규 테넌트는 항상 최신 스키마로 create_all() 되므로, DDL은 문제 없지만 데이터 백필이 포함된 revision은 건너뛰게 됩니다.  
     현실적으로 이게 문제가 되는 시점은 "기존 테넌트에는 upgrade로 데이터 변환 + 신규 테넌트에는 초기값 세팅이 필요" 할 때인데, 이 경우:
  - upgrade의 데이터 로직을 멱등하게 작성하면 stamp 없이 upgrade head로 통일 가능
  - 또는 provisioning에서 stamp 대신 upgrade head를 직접 실행하는 방식으로 전환
    지금 단계에선 DDL만 다룰 가능성이 높으니 현행 유지하고, 데이터 마이그레이션이 생기는 시점에 전환해도 충분합니다.
- [ ] part, part_revision에 status 없음 <- 승인 관리 워크플로우가 있어야함 (draft, released 등) (lifecycle_state 는 제품 수명 주기)
- [ ] 도면 업로드 분리 및 업로드 url 관리 방안 (part 상세에서 업로드, part 목록화면에서 업로드)
- [ ] 도면 client 에 batch 생성
- [ ] 도면 client 연결
