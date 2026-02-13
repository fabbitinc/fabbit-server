# 5. validate severity 정책(에러/경고/정보) 권장안

권장: SHACL의 severity(Info/Warning/Violation) 방식을 매핑 validate에도 차용합니다.
- SHACL 1.2 Core: https://www.w3.org/TR/shacl12-core/

## ERROR (confirm 또는 synthesis를 막아야 함)
- 타입 변환 불가(예: float 요구인데 값이 문자열/혼합으로 변환 불가)
- ACTIVE 관계의 endpoint 키 누락(그래프 적재 불가)
- merge key 부재/충돌(Part key가 비어 업서트 불가)
- 관계 속성 매핑이 있는데 관계 슬롯이 INACTIVE/UNMAPPED(모순)

## WARNING (confirm 허용, 강한 UI 표시)
- ext로 남아 있지만 ontology 후보 점수가 높은 컬럼
- 의미 다의성(단가/원가/판매가)로 오해 가능 → 사용자 확인 필요
- 단위 변환 필요/정밀도 손실 가능
- 동일 컬럼의 중복 매핑(의도 확인)

## INFO (안내/개선 제안)
- 자동 정규화 수행(예: “1,000원” → 1000)
- 후보 존재(낮은 점수)
- optional 속성 미매핑

## confirm vs synthesis 게이트(권장)
- confirm 저장: WARNING까지 허용(사용자 의도/편집 상태 기록)
- synthesis 실행: ERROR가 0일 때만 허용(그래프 정합성 보호)
