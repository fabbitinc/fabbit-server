# TODO — Mapping Editor Phase 2 (확장 편집)

> 전제: Phase 1 완료 후 진행.

## 1) 편집 기능 확장

- [ ] 관계 추가 허용
  - 허용 rel_type은 ontology 정의값으로 제한
- [ ] 관계 삭제 허용
  - 삭제 영향도(합성 결과 변화) 경고 표시
- [ ] ext → 관계 속성 승격 허용
  - 예: `_ext_unit_price_krw`를 `SUPPLIED_BY.unit_cost`로 승격
- [ ] ext 전용 API 도입 여부 확정
  - 도입 트리거: 협업 동시 편집, 감사/이력 요구, 충돌 해결 요구
  - 미도입 시: 기존 `validate -> confirm` 단일 플로우 유지

## 1-1) ext 전용 API 초안

- [ ] ext CRUD 엔드포인트 초안 정의
  - 예: `POST /mappings/{id}/ext`, `PATCH /mappings/{id}/ext/{key}`, `DELETE /mappings/{id}/ext/{key}`
- [ ] ext 전용 API와 기존 validate/confirm 호환 정책 정의
  - `extended_properties` 단일 소스 유지, 서버 정규화 결과를 최종 저장 기준으로 고정
- [ ] ext 전용 API 응답 스키마/에러 코드 초안 정의
  - `_ext_` 네이밍 위반, source column 누락, 타입 파싱 경고 코드 표준화

## 2) 고급 검증/추천

- [ ] 관계 생성 추천 로직(헤더 패턴 + 데이터 샘플 기반)
- [ ] 관계 속성 타입 추천(정수/실수/문자)
- [ ] 충돌 해소 가이드 자동 제안

## 3) 고급 UI

- [ ] 전문가 모드(JSON editor)
- [ ] 변경 diff 뷰 (`before`/`after`)
- [ ] 변경 이력(버전) 및 되돌리기

## 4) 운영/품질

- [ ] 매핑 버전 관리 모델 추가
- [ ] 변경 감사 로그(누가/언제/무엇을)
- [ ] Phase 2 기능에 대한 e2e 회귀 테스트

## 완료 기준 (DoD)

- [ ] 파워유저가 UI에서 관계 구조까지 완결적으로 조정 가능
- [ ] 모든 확장 편집이 서버 검증을 통과한 경우에만 확정 가능
- [ ] 변경 이력 추적/복구가 가능
