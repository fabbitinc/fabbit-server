# C. 데이터 모델 제안(노드/관계/키)

## C1) Apache AGE 저장/쿼리 전제(설계에 직접 영향)

- AGE에서 **graph를 만들면 Postgres namespace(schema)가 생성**되고, vertex/edge label은 그 namespace 아래 테이블로 생성됩니다. `_ag_label_vertex`, `_ag_label_edge`가 부모 테이블이며 `create_vlabel()/create_elabel()` 또는 Cypher `CREATE`로 label 테이블이 만들어집니다. ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  
- AGE의 property는 `agtype`(JSON superset, JsonB의 커스텀 구현)로 저장됩니다. ([age.apache.org](https://age.apache.org/age-manual/master/intro/types.html))  
- Cypher는 `ag_catalog.cypher(graph_name, query_string, parameters)` 형태의 SQL 함수로 실행합니다. ([age.apache.org](https://age.apache.org/age-manual/v0.6.0/intro/cypher.html))  

## C2) 최소 스키마(권장) — 노드(Label)

아래는 “MVP에서 필요한 최소 + 이후 확장 가능한” 기준입니다. 핵심은 **모든 엔티티에 안정적인 `key`**를 두고 `MERGE` 기반 업서트를 가능하게 하는 것입니다. ([age.apache.org](https://age.apache.org/age-manual/master/clauses/merge.html))  

### 1) Project
- Label: `Project`
- Key: `project_key = "{tenant_id}:{project_code}"`  
- Properties(예):  
  - `tenant_id`, `project_code`, `name`, `created_at`, `status`

### 2) Part
- Label: `Part`
- Key: `part_key = "{tenant_id}:{part_no_norm}"`
- Properties(예):  
  - `tenant_id`, `part_no_raw`, `part_no_norm`, `description`, `part_type`, `default_uom`

### 3) PartRevision
- Label: `PartRev`
- Key: `partrev_key = "{tenant_id}:{part_no_norm}@{rev_norm}"`  
- Properties(예):  
  - `tenant_id`, `part_no_norm`, `rev_raw`, `rev_norm`, `lifecycle`(draft/released/obsolete), `effective_from`, `source_job_id`

### 4) Drawing
- Label: `Drawing`
- Key: `drawing_key = "{tenant_id}:{drawing_no_norm}"`
- Properties(예):  
  - `tenant_id`, `drawing_no_raw`, `drawing_no_norm`, `title`

### 5) DrawingRevision
- Label: `DrawingRev`
- Key: `drawingrev_key = "{tenant_id}:{drawing_no_norm}@{rev_norm}"`  
- Properties(예):  
  - `tenant_id`, `drawing_no_norm`, `rev_norm`, `file_uri`, `file_hash`, `extraction_method`(native/ocr/dxf), `ocr_avg_conf`, `source_job_id`

(선택) **6) ImportJob / SourceFile (provenance 강화용)**  
- P0에서는 관계형 테이블로만 가지고, 그래프에는 `source_job_id`, `source_row_id`를 property로 찍는 방식이 더 단순합니다.

## C3) 최소 스키마 — 관계(Relationship)

### A) 프로젝트 스코프
- `(Project)-[:HAS_PART]->(Part)`
- `(Project)-[:HAS_DRAWING]->(Drawing)`

### B) Revision 모델
- `(Part)-[:HAS_REV]->(PartRev)`
- `(Drawing)-[:HAS_REV]->(DrawingRev)`

### C) BOM 계층(핵심)
- `(PartRev)-[:BOM_ITEM]->(PartRev)`  
- Edge properties(예):  
  - `tenant_id`, `row_key`, `qty`, `uom_norm`, `uom_raw`, `find_no`, `line_no`, `refdes`, `scrap_factor`, `source_job_id`, `source_row_id`, `confidence`

### D) 도면-품번 링크
- `(PartRev)-[:HAS_DRAWING]->(DrawingRev)`  
- Edge properties(예): `match_rule`, `match_confidence`, `evidence`

## C4) BOM 계층 관계 표현 시 주의점(실무 함정)

### 1) 중복 BOM 라인(같은 child가 여러 번 등장)
- 단순히 `(parent)-[:BOM_ITEM]->(child)`를 “(parent, child) 유일”로 만들면, 서로 다른 find_no/refdes를 덮어씁니다.  
- 해결: edge를 `row_key`(예: `{job_id}:{sheet}:{row_index}` 또는 canonical row hash)로 식별하고, `MERGE (parent)-[r:BOM_ITEM {row_key: ...}]->(child)`로 생성하세요. `MERGE`는 “패턴 전체가 매치되거나 생성”이므로 key 설계가 중요합니다. ([age.apache.org](https://age.apache.org/age-manual/master/clauses/merge.html))  

### 2) 사이클 방지(제조 BOM은 일반적으로 DAG 기대)
- ingest 시점에 cycle 검사(아래 C6 예시 쿼리) + 실패 시 `NEEDS_REVIEW`로 분기  
- “cycle이 허용되는 특수 BOM(재사용/조립치환)”이 있으면 P2에서 별도 모델링(대체부품, phantom 등)로 다루는 게 안전합니다(추정).

### 3) Revision 스냅샷
- “Part”에 rev를 property로 넣고 계속 갱신하면 rev 비교가 불가능해집니다.  
- 최소한 `PartRev`를 별도 노드로 두고 BOM edge를 `PartRev` 간으로 두면 rev 비교/추적이 가능합니다.

## C5) 인덱싱/쿼리 패턴(AGE) — MVP에서 바로 쓰는 수준

### (1) 기본 인덱스(필수)
AGE는 기본적으로 인덱스를 자동으로 만들어주지 않는다고 명시되어 있어, label 생성 후 바로 인덱스를 잡는 게 안전합니다. ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

- Vertex/Edge 공통
  - `id` BTREE
  - `properties` GIN (광범위 property 검색용)
- Edge는 traversal 성능 때문에 `start_id`, `end_id` BTREE가 매우 중요합니다. ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

예시(그래프 네임스페이스가 `fabbit_t1`인 경우):
```sql
-- Vertex
CREATE INDEX ON fabbit_t1."PartRev" USING BTREE (id);
CREATE INDEX ON fabbit_t1."PartRev" USING GIN (properties);

-- Edge (BOM_ITEM)
CREATE INDEX ON fabbit_t1."BOM_ITEM" USING BTREE (id);
CREATE INDEX ON fabbit_t1."BOM_ITEM" USING GIN (properties);
CREATE INDEX ON fabbit_t1."BOM_ITEM" USING BTREE (start_id);
CREATE INDEX ON fabbit_t1."BOM_ITEM" USING BTREE (end_id);
```
([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

### (2) “자주 쓰는 키”에 대한 targeted BTREE 인덱스(필수)
Microsoft 문서 기준으로, `WHERE n.Name='Alice'` 같은 패턴이 `agtype_access_operator`를 타며, 특정 key에 대해 BTREE 인덱스를 만들 수 있습니다. ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

예시(PartRev의 `key`를 자주 조회):
```sql
CREATE UNIQUE INDEX partrev_key_uq
ON fabbit_t1."PartRev"
USING BTREE (
  ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"key"'::agtype])
);
```
([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

### (3) 쿼리 작성 규칙(인덱스 활용을 위해)
아래 두 쿼리는 평가 방식이 다르다고 문서에 명시되어 있습니다.  
- `MATCH (n:Customer {Name:'Alice'})` → `properties @> ...`  
- `MATCH (n:Customer) WHERE n.Name='Alice'` → `agtype_access_operator(...) = ...` ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

즉, **정확 일치/키 조회는 `WHERE n.key = ...` 스타일을 기본으로** 두고, GIN을 기대하는 “부분 포함” 류만 `{...}` 또는 `@>` 계열을 쓰는 식으로 팀 컨벤션을 잡는 게 좋습니다.

### (4) 자주 쓰는 조회 쿼리 예시

> 아래는 “예시”이며, 실제 property명/graph명은 컨벤션에 맞춰 고정하세요.

#### a) BOM 트리(전개)
```sql
SELECT * FROM cypher('fabbit_t1', $$
  MATCH (root:PartRev)
  WHERE root.key = 't1:P100@A'
  MATCH (root)-[r:BOM_ITEM*1..10]->(child:PartRev)
  RETURN root.key, child.key
$$) AS (root_key agtype, child_key agtype);
```
- 운영 팁: depth(`1..10`)를 API에서 제한하고, pagination/limit을 둬야 합니다(대형 BOM에서 폭발).

#### b) Where-used(영향도)
```sql
SELECT * FROM cypher('fabbit_t1', $$
  MATCH (c:PartRev)
  WHERE c.key = 't1:P200@B'
  MATCH (p:PartRev)-[:BOM_ITEM*1..10]->(c)
  RETURN DISTINCT p.key
$$) AS (parent_key agtype);
```

#### c) Revision 비교(차이 계산)
- AGE 내부에서 diff를 모두 계산하기보다, **두 rev의 BOM 라인 목록을 뽑아 애플리케이션에서 set diff** 하는 게 MVP에선 단순합니다(AGE에서 aggregation/ordering이 SQL보다 느리다는 보고가 있어, 고급 비교는 P1/P2로 미루는 게 안전). ([github.com](https://github.com/apache/age/issues/2194))  

```sql
-- Rev A children
SELECT * FROM cypher('fabbit_t1', $$
  MATCH (p:PartRev)-[r:BOM_ITEM]->(c:PartRev)
  WHERE p.key='t1:P100@A'
  RETURN c.key, r.qty, r.uom_norm, r.row_key
$$) AS (child_key agtype, qty agtype, uom agtype, row_key agtype);
```

### (5) 알려진/보고된 성능 리스크(“추정 포함”)
- 일부 패턴에서 인덱스가 기대대로 사용되지 않는 이슈 리포트가 있습니다(버전/쿼리 형태에 따라 달라질 수 있음). 따라서 **P0부터 `EXPLAIN`을 Cypher 내부에서 실행하여** 인덱스 사용 여부를 검증하는 절차를 넣는 게 안전합니다. ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

## C6) 멀티테넌시/권한 경계 설계(권장 패턴)

### 권장(P0): Graph-per-tenant(네임스페이스 분리)
- AGE는 graph 생성 시 Postgres namespace가 생기므로, tenant마다 `fabbit_t_{tenant}` graph를 만들면 물리적 경계가 단순해집니다. ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  
- FastAPI에서 tenant별 graph_name을 결정하여 `cypher(graph_name, ...)` 호출  
- 장점: 데이터 격리 단순, 삭제/이관 쉬움, 실수로 cross-tenant 조회할 위험 감소  
- 단점: tenant 수가 매우 많으면 운영/DDL(인덱스) 관리가 부담(추정)

### 대안(P1+): Single graph + `tenant_id` property + (RLS)
- Postgres RLS는 row 단위 접근 제어 기능입니다. ([postgresql.org](https://www.postgresql.org/docs/current/ddl-rowsecurity.html))  
- 다만, **AGE의 `cypher()`가 RLS를 완전하게 “기대대로” 적용하는지는 환경별 검증이 필요**합니다(추정).  
- 안전한 운영 방법(권장): 그래프 조회는 항상 애플리케이션에서 `WHERE n.tenant_id = $tenant_id`를 강제하고, 관계형 canonical 테이블은 RLS로 defense-in-depth를 추가.

RLS를 쓸 때 함수/뷰의 보안 속성(SECURITY INVOKER/DEFINER)이 권한에 영향을 주므로(특히 security definer는 RLS 우회 위험), 보안 설계 체크리스트에 포함시키는 게 좋습니다. ([postgresql.org](https://www.postgresql.org/docs/current/sql-createfunction.html))  

## C7) Idempotent upsert 및 트랜잭션 전략(실무 권장)

### (1) 관계형(파이프라인 메타/캐노니컬) 업서트
- `INSERT ... ON CONFLICT DO UPDATE`를 사용해 job/file/row를 idempotent하게 적재합니다. Postgres는 ON CONFLICT를 “deterministic”로 다루며, 동일 row를 중복 업데이트하려 하면 에러가 날 수 있으니(동일 statement 내 중복) 입력을 미리 dedupe 해야 합니다. ([postgresql.org](https://www.postgresql.org/docs/current/sql-insert.html?utm_source=chatgpt.com))  

### (2) 그래프 업서트
- 노드: `MERGE (n:PartRev {key: ...}) SET ...`  
- BOM edge: `row_key` 포함한 패턴으로 `MERGE`  
- `MERGE`는 “패턴 전체가 매치되거나 전체가 생성”이므로, **노드 MERGE와 edge MERGE를 분리**하는 게 안전합니다. ([age.apache.org](https://age.apache.org/age-manual/master/clauses/merge.html))  

예시:
```sql
SELECT * FROM cypher('fabbit_t1', $$
  MERGE (p:PartRev {key:'t1:P100@A'})
  SET p.tenant_id='t1', p.part_no_norm='P100', p.rev_norm='A'

  MERGE (c:PartRev {key:'t1:P200@B'})
  SET c.tenant_id='t1', c.part_no_norm='P200', c.rev_norm='B'

  MERGE (p)-[r:BOM_ITEM {row_key:'job123:sheet1:42'}]->(c)
  SET r.qty=2, r.uom_norm='EA', r.source_job_id='job123'
  RETURN r
$$) AS (r agtype);
```
([age.apache.org](https://age.apache.org/age-manual/master/clauses/merge.html))  

### (3) 트랜잭션(대용량 BOM) 권장
- “파일 1개 = 트랜잭션 1개”는 큰 BOM에서 롤백 비용이 큽니다.  
- 권장: **N줄(예: 500~2,000 라인) 단위 배치 커밋** + 체크포인트 저장(관계형 테이블에 `last_row_processed`)  
- 장애 복구: 마지막 체크포인트 이후 라인부터 재처리
