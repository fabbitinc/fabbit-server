# TODO 03 — 온톨로지 구조 확장(B안): BOMLine 엔티티 도입

> 선행 조건: `todo/foundation/01-direction.md`, `todo/foundation/02-reliability.md`의 핵심 항목 합의 후 진행
> 트리거: effective_date, revision별 BOM, 라인 단위 변경이력 추적 요구가 실제로 발생할 때 진행

## 진행 원칙

- [ ] 전면 교체보다 단계적 전환(공존 기간)을 기본 전략으로 사용
- [ ] 기존 `CONSISTS_OF` 읽기 경로를 즉시 제거하지 않고, migration 완료 후 단계적으로 제거
- [ ] 전환 기간에 신규 적재 정책(BOMLine 기반)과 기존 조회 정책의 호환성 테스트 유지

## 온톨로지 변경

- [ ] `base_ontology.py` — BOMLine 노드 라벨 추가 (merge key: `bom_line_id`)
  - 속성: quantity, sequence, reference_designator, find_number, effective_from, effective_to
- [ ] `base_ontology.py` — 관계 타입 재정의
  - `CONSISTS_OF` (Part → Part) 제거
  - `HAS_BOM_LINE` (Part → BOMLine) 추가
  - `REFERENCES_PART` (BOMLine → Part) 추가

## 합성 파이프라인

- [ ] `synthesis/service.py` — BOMLine 노드 생성 로직 구현
  - CONSISTS_OF 대신 BOMLine 노드 MERGE + HAS_BOM_LINE/REFERENCES_PART 관계 2개 생성
- [ ] `ontology/service.py` — LLM 프롬프트에 BOMLine 매핑 규칙 반영

## 아이템 조회 쿼리 (3-hop 재작성)

- [ ] `item/repository.py` — `get_children`: `(p)-[:HAS_BOM_LINE]->(bl)-[:REFERENCES_PART]->(child)`
- [ ] `item/repository.py` — `get_parents`: `(parent)-[:HAS_BOM_LINE]->(bl)-[:REFERENCES_PART]->(p)`
- [ ] `item/repository.py` — `get_bom_paths`: 가변 깊이 경로 쿼리 변경
- [ ] `item/repository.py` — `list_parts`: BOMLine 경유 관계 반영
- [ ] `item/service.py` — `_build_bom_tree()` 경로 파싱 변경, 응답에 bom_line_id 추가

## 품질 검사

- [ ] `activation/repository.py` — 헬스체크 Cypher 2곳 수정 (BOMLine 경유)

## 데이터 마이그레이션

- [ ] 마이그레이션 스크립트 작성: 기존 CONSISTS_OF 엣지 → BOMLine 노드 + 2관계 변환
- [ ] 기존 테넌트 그래프 일괄 마이그레이션 실행 및 검증
- [ ] 무결성 검증 체크리스트 수행
  - 전/후 parent-child 연결 건수 동등성
  - 샘플 부품의 BOM depth/path 비교
  - 누락/중복 BOMLine 탐지 리포트 생성

## 테스트/시드

- [ ] `seed_data.py` — BOMLine 기반 시드 데이터 변경
- [ ] `scripts/test_full_flow.sh` — BOMLine 검증 단계 추가

## 완료 기준 (Definition of Done)

- [ ] 기존 주요 조회 API에서 BOM 구조 결과가 전환 전과 기능적으로 동일
- [ ] effective/revision 요구를 수용할 수 있는 스키마로 확장 완료
- [ ] 마이그레이션 롤백/재실행 절차가 문서화됨
