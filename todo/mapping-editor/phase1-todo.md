# TODO — Mapping Editor Phase 1 (MVP, 제약 편집)

> 결정: 관계 추가/삭제는 막고, 기존 관계의 endpoint/속성 source 변경만 허용한다.

## 0) 정책 고정 (Frontend 합의 반영)

- [ ] 관계 유효성 규칙 분리 적용
  - endpoint(`from_columns`/`to_columns`)는 엄격 검증
  - 관계 속성(`properties`)은 완화 검증 + required만 강제
- [ ] restore/toggle은 서버 revalidate 성공 시에만 반영
- [ ] confirm 전에 validate 강제 + normalized_mapping 동기화
- [ ] `dismissed_reason` 코드 체계 도입
  - 예: `missing_from_endpoint`, `missing_to_endpoint`, `missing_required_rel_property`

## 1) 백엔드 API

- [ ] `POST /api/v1/mappings/validate` 추가
  - 입력: `upload_id`, `sheet_name`, `mapping`
  - 출력: `normalized_mapping`, `errors`, `warnings`, `impact_summary`
- [ ] `POST /api/v1/mappings/confirm` 강화
  - validate 통과본만 저장하도록 서버 가드레일 적용

## 2) 검증 규칙 (서버)

- [ ] merge key 누락 차단
- [ ] self-loop 관계에서 from/to source column 동일 사용 차단
- [ ] 관계 endpoint 컬럼 존재 여부 검증
- [ ] relation property source column 존재 여부 검증 (required 우선)
- [ ] `data_type` 불일치 경고 (`"2 EA"` 등 숫자 파싱 불가)
- [ ] ext naming 규칙 검증 (`_ext_` prefix + snake_case)

## 3) 프론트 편집 범위

- [ ] 컬럼 매핑 수정: target property 변경, 제외/복원, 타입 변경
- [ ] base ↔ ext 전환 허용
- [ ] 관계 매핑 수정: 기존 relation의 endpoint/source 변경 허용
- [ ] 관계 추가/삭제 비활성화
- [ ] ext를 관계 속성으로 승격하는 기능 비활성화

## 4) UX 가드레일

- [ ] 컬럼 변경 시 관계 영향(깨짐/경고) 즉시 표시
- [ ] validate 실패 시 confirm 비활성화
- [ ] 경고는 확인 후 저장 가능, 에러는 저장 불가

## 5) 테스트

- [ ] validate 서비스 단위 테스트
  - self-loop 충돌
  - merge key 누락
  - ext naming 위반
  - 숫자 파싱 경고
- [ ] API 테스트
  - validate 실패/성공 케이스
  - confirm 시 validate 강제 여부

## 완료 기준 (DoD)

- [ ] 화면에서 컬럼/base-ext/관계 endpoint 수정이 가능하다
- [ ] 관계 추가/삭제 없이도 실사용 수정 요구를 처리한다
- [ ] 잘못된 매핑은 validate 단계에서 서버가 확실히 차단한다
