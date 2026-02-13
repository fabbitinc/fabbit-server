# TODO — B안: BOMLine 엔티티 도입

> 트리거: effective_date, revision별 BOM 등 실제 필요성이 생길 때 진행

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

## 테스트/시드

- [ ] `seed_data.py` — BOMLine 기반 시드 데이터 변경
- [ ] `scripts/test_full_flow.sh` — BOMLine 검증 단계 추가
