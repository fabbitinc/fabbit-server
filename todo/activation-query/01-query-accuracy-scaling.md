# TODO — 자연어 질의 정확도 개선 및 대규모 확장 계획

> 목표: 자연어 → Cypher 변환의 정확도를 높이고, 노드 수가 수만 개 이상으로 증가해도 일관된 질의 품질을 유지하는 구조를 만든다.
> 기준 파일: `app/modules/activation/service.py`, `app/modules/activation/repository.py`

## 현재 문제

- LLM이 한글 고유명사를 영문으로 변환하여 WHERE 필터 생성 (예: "미스미코리아" → `CONTAINS 'misumi'`)
- 프롬프트에 스키마 + 카운트만 제공하고, 실제 노드 값을 제공하지 않음
- retry 프롬프트의 `toLower + CONTAINS` 가이드가 한글 데이터에서 역효과

---

## Phase 1 — 즉시 적용 (프롬프트 규칙 강화 + Low-cardinality 값 제공)

> 추가 인프라 없이 현재 구조에서 개선. 노드 수천 개 이하 규모 대응.

### 1-A. 프롬프트 규칙 강화

- [ ] Cypher 생성 프롬프트에 규칙 추가
  - "질문에 포함된 고유명사(회사명, 부품명 등)는 원문 그대로 WHERE 조건에 사용하라. 영문 변환 금지"
  - "한글 데이터에서는 toLower()가 무의미하므로 CONTAINS만 사용하라"
  - 기준 파일: `app/modules/activation/service.py` (`_build_tenant_query_prompt`)

- [ ] retry 프롬프트 수정
  - "toLower + CONTAINS" 가이드를 "CONTAINS (원문 유지)" 로 변경
  - 기준 파일: `app/modules/activation/service.py` (`_build_zero_result_retry_prompt`)

### 1-B. Low-cardinality 라벨 값 목록 제공

- [ ] 참조 데이터 라벨(Supplier, Material, Drawing) distinct 값을 프롬프트에 포함
  - 카디널리티 임계값: 100개 이하인 라벨만 대상
  - repository에 `list_distinct_values(db, graph_name, label, property)` 함수 추가
  - 기준 파일: `app/modules/activation/repository.py`

- [ ] `_build_tenant_query_prompt`에 값 목록 섹션 추가
  ```
  ## 실제 데이터 값 (WHERE 조건에 이 값을 그대로 사용하세요)
  Supplier.name: 미스미코리아, 대한볼트, 삼성기계, 한국정밀, NSK코리아
  Material.name: SUS304, SM45C, ...
  ```
  - 기준 파일: `app/modules/activation/service.py`

### 완료 기준

- [ ] "미스미코리아에서 공급받는 부품" 질의가 정확한 결과 반환
- [ ] 참조 데이터 라벨 필터링 질의의 정확도 90% 이상

---

## Phase 2 — Entity Linking (중규모 확장)

> Part 노드가 수천 개 이상일 때 대응. DB 기반 유사 검색 도입.

### 2-A. PostgreSQL Full-text / Trigram 검색 도입

- [ ] `pg_trgm` 확장 활성화 (이미 AGE용 PostgreSQL 사용 중)
  - `CREATE EXTENSION IF NOT EXISTS pg_trgm`

- [ ] 엔티티 검색 함수 구현
  - 질문에서 추출한 키워드로 노드 속성 유사도 검색
  - `similarity()` 또는 `word_similarity()` 함수 활용
  - 검색 대상: 각 라벨의 merge key 속성 (Part.part_number, Part.name, Supplier.name 등)
  - 기준 파일: `app/modules/activation/repository.py`

- [ ] AGE 노드 속성을 검색 가능한 형태로 인덱싱
  - 방법 A: AGE 노드에서 주기적으로 RDB 테이블에 동기화 (materialized view)
  - 방법 B: 인제스션 시점에 검색용 테이블에 동시 적재
  - 트레이드오프: A는 구현 간단하나 지연 있음, B는 실시간이나 인제스션 로직 복잡해짐

### 2-B. 2-stage 질의 파이프라인

- [ ] Stage 1: 질문 → 고유명사 추출
  - 방법 A: LLM으로 엔티티 추출 (정확도 높으나 레이턴시/비용 추가)
  - 방법 B: 간단한 규칙 기반 추출 (형태소 분석 또는 NER)
  - 권장: 초기에는 LLM 기반, 패턴 축적 후 규칙 기반으로 전환

- [ ] Stage 2: 추출된 고유명사 → DB 유사 검색 → 실제 값 확정

- [ ] Stage 3: 확정된 엔티티 값을 프롬프트에 포함하여 Cypher 생성

### 완료 기준

- [ ] Part 이름/품번으로 검색 시 정확한 매칭 (오타 포함 유사 검색 지원)
- [ ] 엔티티 연결 실패 시 "해당 엔티티를 찾을 수 없습니다. 유사한 항목: ..." 안내 가능

---

## Phase 3 — Few-shot RAG (대규모 확장)

> 질의 패턴이 다양해지고, Cypher 생성 정확도를 추가로 높여야 할 때 도입.

### 3-A. 질문-Cypher 쌍 저장소

- [ ] 성공한 질의(결과 > 0건)의 질문-Cypher 쌍을 RDB에 저장
  - 테이블: `query_examples(id, question, cypher, result_count, created_at)`
  - 사용자 피드백(좋아요/싫어요) 컬럼 추가 고려
  - 기준 파일: `app/modules/activation/models.py`

- [ ] 질문 임베딩 생성 및 저장
  - 방법 A: PostgreSQL `pgvector` 확장 (추가 인프라 없음)
  - 방법 B: 외부 벡터 DB (Pinecone, Qdrant 등)
  - 권장: pgvector 우선 (PostgreSQL 이미 사용 중)

### 3-B. 유사 예시 검색 및 프롬프트 주입

- [ ] 질문 입력 시 유사도 top-3 예시 검색
- [ ] few-shot 예시를 Cypher 생성 프롬프트에 포함
  ```
  ## 유사 질의 예시
  Q: "한국정밀에서 납품하는 부품 목록"
  Cypher: MATCH (p:Part)-[:SUPPLIED_BY]->(s:Supplier) WHERE s.name = '한국정밀' RETURN ...
  ```

### 3-C. Metadata Catalog

- [ ] 라벨별 통계 요약 캐시 구축
  - 카디널리티, 속성별 값 형식 패턴, 샘플 값
  - 인제스션 완료 시점에 자동 갱신
  - 프롬프트 토큰 예산 내에서 동적으로 포함

### 완료 기준

- [ ] 유사 질문 패턴에 대해 일관된 Cypher 생성 (동일 패턴 반복 실패 방지)
- [ ] 새로운 질의 패턴 추가 시 별도 프롬프트 수정 없이 자동 학습

---

## 단계별 도입 기준

| 단계 | 도입 시점 | 핵심 지표 |
|------|----------|----------|
| Phase 1 | **즉시** | 참조 데이터 필터링 정확도 개선 |
| Phase 2 | Part 노드 1,000개+ 또는 필터링 실패율 20%+ | 엔티티 매칭 정확도 95%+ |
| Phase 3 | 질의 패턴 100종+ 또는 Cypher 생성 실패율 15%+ | 반복 질의 정확도 99%+ |
