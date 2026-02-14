# TODO 01 — 최신 매핑 기반 Item 업로드

> 목표: 사용자는 `mapping_id`를 알 필요 없이, 조직의 최신 매핑으로 엑셀 업로드 시 Item 생성이 가능해야 한다.

## 지금 구현 범위 (Phase A)

- [x] 합성 시작 API 입력을 `mapping_id`에서 `upload_id` 중심으로 전환
  - 기준 파일: `app/modules/synthesis/schemas.py`
- [x] 조직 최신 매핑 1건 자동 선택 로직 추가
  - 기준 파일: `app/modules/synthesis/repository.py`, `app/modules/synthesis/service.py`
- [x] 업로드 상태 검증 추가 (`UPLOADED`만 허용)
  - 기준 파일: `app/modules/synthesis/service.py`
- [x] 다중 파일 배치 합성 시작 API를 프로젝트 스코프로 추가 (`/api/v1/projects/{project_id}/synthesis/batch`)
  - 기준 파일: `app/api/v1/tenant/project_router.py`, `app/modules/synthesis/service.py`
- [x] 배치 진행상황 조회 API 추가 (`/api/v1/synthesis/batches/{batch_id}`)
  - 기준 파일: `app/api/v1/tenant/synthesis_router.py`, `app/modules/synthesis/service.py`
- [x] 매핑 선택 우선순위 적용
  - `project 최신 매핑 -> org 최신 매핑`
  - 기준 파일: `app/modules/synthesis/repository.py`, `app/modules/synthesis/service.py`

## mapping_id 관리법 (내부 운영)

- [ ] 사용자 API에서 `mapping_id` 비노출 유지
  - 내부 Job에는 `mapping_id`를 계속 저장해 재현성 확보
- [ ] 합성 Job 로그 표준 키에 `mapping_id` 고정
  - 재처리/장애 분석 시 동일 매핑 기준 확인 가능
- [ ] 매핑 삭제 정책 정의
  - 참조 중인 Job가 있을 때 soft-delete 또는 비활성화 정책 필요

## 추후 조직/프로젝트별 관리 (Phase B)

- [ ] 조직 기본 매핑(active 1개) + 프로젝트 오버라이드(active 1개) 구조 설계
- [ ] 매핑 선택 우선순위 규칙 정의
  - `project active -> org active -> 없음(오류)`
- [ ] 프로젝트별 헤더 패턴 캐시로 자동 선택 정확도 개선

## mapping diff 관리 (Phase C)

- [ ] 매핑 버전 불변 저장 + active 포인터 분리
- [ ] 변경 diff 계산 규칙 정의
  - column/relation/extended 추가·삭제·타입변경 구분
- [ ] diff 기반 영향도 요약(누락 merge key, 비활성 컬럼 수 등) 제공
- [ ] 롤백 API(이전 버전을 active로 전환) 설계

## 완료 기준 (Definition of Done)

- [ ] 사용자는 업로드 파일만 지정해서 Item 합성을 시작할 수 있다
- [ ] 시스템은 `project 최신 -> org 최신` 우선순위로 Job를 생성하고 상태를 추적할 수 있다
- [ ] 실패 시 어떤 매핑으로 실행되었는지 운영자가 즉시 확인할 수 있다
