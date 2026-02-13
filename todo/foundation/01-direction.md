# TODO 01 — 방향성 정렬 (Research 기반)

> 목표: MVP 단계에서 "무엇을 먼저 고정할지"를 명확히 해 기능 확장을 안정적으로 받는 구조를 만든다.
> 범위: 아키텍처/데이터 흐름/온톨로지-쿼리 일관성. 운영 고도화(큐, 대규모 재처리)는 제외.

## A. 레이어/경계 고정

- [ ] 라우터-서비스-리포지토리 경계 점검 체크리스트를 PR 템플릿 또는 개발 문서에 명시
  - 기준 파일: `app/api/v1/tenant/*_router.py`, `app/modules/*/service.py`, `app/modules/*/repository.py`
- [ ] tenant 경계 규칙을 "테스트 가능한 규칙"으로 문서화
  - `get_tenant_db` 필수 사용, tenant 라우터의 `get_db` 금지
  - 기준 파일: `app/api/deps.py`

## B. 온톨로지/매핑/합성 일관성 정렬

- [ ] 온톨로지 SSoT 변경 절차 정의
  - 변경 순서: `base_ontology.py` → `ontology/service.py` 프롬프트 → `synthesis/service.py` 반영 → `item/repository.py` 조회 반영
  - 기준 파일: `app/modules/ontology/base_ontology.py`, `app/modules/ontology/service.py`
- [ ] "관계 매핑 최소 요건"을 명문화
  - self-loop(Part→Part)에서 from/to source column 분리 필수
  - 관계 생성 조건 불충분 시 관계 생성을 건너뛰는 정책 고정
  - 기준 파일: `app/modules/ontology/service.py`
- [ ] `_ext_` 확장 속성 정책 고정
  - 저장 규칙, 질의 가능 범위, UI/응답 노출 기준 정의
  - 기준 파일: `app/modules/ontology/base_ontology.py`, `app/modules/activation/service.py`

## C. 데이터 파이프라인 단계 정의 (MVP 버전)

- [ ] Upload → Mapping → Synthesis → Activation 단계별 "입력/출력/실패코드" 표 작성
  - 기준 파일: `app/modules/upload/service.py`, `app/modules/mapping/service.py`, `app/modules/synthesis/service.py`, `app/modules/activation/service.py`
- [ ] 상태값 의미 사전 정리
  - Upload/Synthesis/Drawing status의 전이 조건을 하나의 문서로 통합
  - 기준 파일: `app/modules/upload/models.py`, `app/modules/synthesis/models.py`, `app/modules/drawing/models.py`

## D. 쿼리/응답 규약 정렬

- [ ] Activation read-only 정책 강화
  - 허용 키워드/차단 키워드 및 예외 케이스 문서화
  - 기준 파일: `app/modules/activation/service.py`
- [ ] Item 조회 응답 스키마를 온톨로지 속성과 대조
  - 필수/선택 필드와 `_ext_` 노출 규약 정리
  - 기준 파일: `app/modules/item/service.py`, `app/modules/item/schemas.py`

## E. 문서 동기화

- [ ] `docs/design/onboarding/*`와 구현 간 차이(diff) 정리
- [ ] 변경 시 문서 갱신 책임 위치(모듈 오너 또는 PR 작성자) 명시

## 완료 기준 (Definition of Done)

- [ ] 신규 기능 PR에서 레이어 위반/tenant 경계 위반이 재발하지 않는다
- [ ] 온톨로지 변경이 매핑/합성/조회에 일관되게 반영된다
- [ ] 팀 내에서 "우리가 지금 지키는 MVP 규약"을 1개 문서로 합의했다
