# 마이그레이션 추적 인덱스

## 기준

- 비교 기준은 레거시 `../server/app/modules/*/service.py`입니다.
- 레거시에는 이름이 정확히 `usecase.py`인 파일이 없습니다.
- 필요한 경우 `../server/app/use_cases/**`와 조회 전용 `queries/**`를 보조 근거로 사용했습니다.
- 이번 추적은 정적 코드 비교 기준입니다. 통합 테스트, DB 마이그레이션, 운영 데이터 검증은 포함하지 않습니다.

## 전체 판단

- 핵심 CRUD와 상태 전이 로직은 대부분 Spring Boot로 이전되었습니다.
- 다만 비동기 처리, 운영성 배치, 실시간 푸시, 쿼터/과금, AI 기반 보조 기능에서 큰 누락이 남아 있습니다.
- 따라서 현재 상태는 "주요 쓰기 기능은 상당수 이전 완료, 운영 완결성은 아직 부분 이전"으로 보는 편이 맞습니다.

## 도메인별 문서

| 도메인      | 문서                                | 판단                                                                                                                                    |
| ----------- | ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 계정/조직   | `account-organization-migration.md` | 기본 인증/초대/프로필/멤버십, 초기 구독, 스토리지 quota는 복구됐고 일부 인증 방어 차이만 남아 있습니다.                                 |
| 제품 데이터 | `product-data-migration.md`         | Part/파일 첨부, 도면 변환, stale/deleted/orphan 파일 정리 배치는 강하지만 실제 프로필 썸네일 변환, QCAD 배포 포함 작업이 남아 있습니다. |
| 협업        | `collaboration-migration.md`        | 팀/이슈/라벨/읽음 처리는 강하지만 프로젝트 activity 쓰기와 알림 SSE push가 비어 있습니다.                                               |
| AI/그래프   | `ai-graph-migration.md`             | 매핑은 강하지만 activation 자연어 질의와 synthesis의 일반 그래프 폴백/루트 컨텍스트 검증이 아직 축소되어 있습니다.                      |

## 전환 메모

- [v1-removal-plan.md](/Users/seongha.moon/code/projects/fabbit/server2/refacotr/v1-removal-plan.md)
  - Mapping/Synthesis의 V1 제거, V2 승격, 최종 canonical 이름 정리 기준입니다.

## 우선순위 높은 갭

1. Activation 자연어 질의 축소
   - 레거시의 LLM 기반 `sql/graph/hybrid` 질의가 현재는 제한된 휴리스틱 조회로 축소됐습니다.
2. Synthesis 일반 그래프 폴백 및 root context 검증 미완
   - `Drawing`, `DEFINED_BY`, `HAS_ITEM`, 관계 확장 속성 저장은 복구됐지만, 기타 ontology node/relationship fallback과 root context 세부 키 검증은 아직 없습니다.
3. 프로젝트 activity 쓰기 부재
   - 조회 API는 있지만 기록 생산자가 없습니다.

## 범위 밖 또는 별도 확인 필요

- 레거시 `dashboard`, `usage`, `activity`는 이번 기준인 `service.py`/`usecase.py` 범위 밖입니다.
- 현재 Spring에는 해당 조회 API가 존재하므로, 필요하면 다음 단계에서 "조회/리포트 도메인"만 따로 추적하는 편이 맞습니다.
