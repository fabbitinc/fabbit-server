# TODO 02 — 운영 신뢰도 향상 (MVP 다음 단계)

> 목표: 방향성 정렬 후 장애 복구 가능성과 재현 가능성을 높인다.
> 원칙: 과도한 분산 시스템 도입보다, 현재 구조에서 효과가 큰 항목부터 단계적으로 적용한다.

## P0. 최소 신뢰도 (지금 당장 효과 큰 항목)

- [ ] 백그라운드 작업 상태 전이 표준화
  - `PENDING → PROCESSING → COMPLETED | FAILED` 전이 조건과 오류 메시지 규약 통일
  - 기준 파일: `app/modules/synthesis/service.py`, `app/modules/drawing/service.py`
- [ ] 실패 원인 저장 일관화
  - `errors` 필드에 사용자용 메시지/내부 디버그 메시지 분리 저장 정책
  - 기준 파일: `app/modules/synthesis/models.py`, `app/modules/drawing/models.py`
- [ ] 재실행(runbook) 문서화
  - 특정 job 실패 시 확인 순서(DB, S3 key, mapping, graph) 문서 작성

## P1. 멱등성/재처리 안정화

- [ ] 업로드 멱등성 키 도입 검토
  - 후보: `sha256(file)` 또는 `(org_id, file_key, size)` 기반 fingerprint
  - 기준 파일: `app/modules/upload/models.py`, `app/modules/upload/service.py`
- [ ] 합성 재처리 정책 명시
  - 동일 입력 재실행 시 중복 적재를 허용/비허용할지, 기준 키 정의
  - 기준 파일: `app/modules/synthesis/service.py`
- [ ] 그래프 적재 후 검증 훅 추가
  - 최소 검증: 노드/관계 수 증가량, 핵심 라벨 존재 여부
  - 기준 파일: `app/modules/activation/repository.py`

## P1. 관측성 개선

- [ ] job 단위 correlation id 로그 키 고정
  - `job_id`, `org_id`, `upload_id`, `mapping_id`를 공통 키로 사용
  - 기준 파일: `app/modules/synthesis/service.py`, `app/modules/drawing/service.py`
- [ ] OTel span 속성 표준화
  - 단계별 처리시간(파싱/LLM/적재) attribute 키를 통일
  - 기준 파일: `app/main.py`, `app/core/observability.py`

## P2. 큐/워커 전환 (필요 시)

- [ ] `BackgroundTasks` 한계 기준 정의
  - 트래픽/처리시간 임계치 초과 시 큐 도입 트리거 명시
- [ ] Celery(or 대안) PoC
  - 현재 함수 시그니처 유지한 채 task runner만 교체 가능한 구조 설계
  - 기준 파일: `app/api/v1/tenant/synthesis_router.py`, `app/api/v1/tenant/drawing_router.py`
- [ ] retry/backoff/DLQ 정책 문서화

## P2. 테스트 확장

- [ ] 서비스 단위 테스트 확장
  - mapping/synthesis/drawing/activation 핵심 분기 최소 1세트씩
- [ ] 실패/복구 시나리오 테스트 추가
  - S3 다운로드 실패, Cypher 실패, LLM JSON 파싱 실패
- [ ] E2E smoke에 실패 케이스 검증 추가
  - 기준 파일: `scripts/test_full_flow.sh`

## 완료 기준 (Definition of Done)

- [ ] 실패한 작업의 원인/재시도 방법을 운영자가 문서만으로 수행 가능
- [ ] 같은 입력 재처리 시 중복/불일치가 통제 가능
- [ ] 핵심 흐름에서 장애 지점을 로그/트레이스로 10분 내 식별 가능
