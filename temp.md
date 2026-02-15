⏺ 큰 틀은 맞는데 좀 더 구체적으로:

1. 기반 작업: BOM RDS 테이블 + Repository 패턴 정립

- bom_links 테이블 생성 (Part↔Part 관계 RDS화)
- Repository에 RDS+Graph 캡슐화 패턴 적용
- 기존 Synthesis → repository 경유로 변경

2. 기존 코드 수정

- Item 모듈 단순화: children/parents 조회를 RDS JOIN으로 (Graph enrichment 제거)
- Activation: 기존 동작 유지하되 새 구조 반영

3. 신규 기능: Project + Folder + File

- Project 모델 + CRUD API
- Folder 모델 (self-ref) + CRUD API
- File 모델 + 업로드/관리 API
- Project↔Part 관계 (RDS + Graph dual write)
- GET /projects/{id}/items (프로젝트 소속 아이템)

순서

1 → 2 → 3 (의존성 순)

1이 끝나야 2가 깔끔해지고, 1+2가 끝나야 3에서 동일 패턴을 적용할 수 있습니다.

바로 플랜 모드로 들어가서 1번부터 상세 설계할까요?

===

현재 @transactional, uow를 사용하지 않음
