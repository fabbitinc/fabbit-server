# B. 권장 아키텍처 다이어그램(텍스트)

```mermaid
flowchart LR
  U[User/Client] -->|Upload BOM/PDF/DXF| API[FastAPI API]
  U -->|Query| API

  subgraph Storage
    OBJ[(Object Storage: S3/MinIO\nraw + intermediate artifacts\nversioned)]
    PG[(PostgreSQL\n- Relational (jobs, canonical)\n- Apache AGE (graph))]
  end

  subgraph Async
    Q[(Queue: Redis/RabbitMQ)]
    W1[Worker: BOM Parser]
    W2[Worker: PDF Parser/OCR]
    W3[Worker: DXF Parser]
    W4[Worker: Normalizer]
    W5[Worker: Ontology Mapper]
    W6[Worker: Graph Loader]
    W7[Worker: Quality Gates + Synthesis]
    DLQ[(Dead Letter / Needs Review)]
  end

  subgraph Human-in-the-Loop
    HITL[Review UI/Console\n(template mapping, conflicts, low-confidence)]
  end

  subgraph LLM
    LLMsvc[LLM (fallback only)\nstructured output + cache]
  end

  API -->|store raw| OBJ
  API -->|create job + enqueue| PG
  API --> Q

  Q --> W1 --> PG
  Q --> W2 --> PG
  Q --> W3 --> PG

  PG --> W4 --> PG
  PG --> W5 --> PG
  PG --> W6 --> PG
  PG --> W7 --> PG

  W2 -->|low confidence| LLMsvc
  W1 -->|unknown template| LLMsvc
  W7 -->|validation fail| DLQ --> HITL

  API -->|cypher/sql read| PG
  API -->|fetch raw/derived| OBJ
```

## 서비스 경계(ingestion / mapping / synthesis / query)와 책임 분리

### Ingestion(수집)
- FastAPI: 업로드/메타데이터 등록/권한검사/중복 업로드 방지(idempotency key)/job 생성/큐 enqueue  
- 산출물: `raw_file`, `ingestion_job` 레코드 + 객체스토리지 URI(버전 포함)

### Parsing(파싱)
- BOM Parser: XLSX/CSV → “row-wise canonical BOM rows”(원본 row index 포함)  
- PDF Parser/OCR: PDF → 텍스트/테이블/타이틀블록 후보 + OCR TSV(필요 시)  
- DXF Parser: DXF → TEXT/MTEXT/블록 attribute 기반 메타 추출

### Mapping(정규화/온톨로지 매핑)
- Normalizer: unit/qty/rev/part_no 표준화 + confidence 계산  
- Mapper: canonical rows → graph node/edge change-set 생성(업서트 키 포함)

### Synthesis(파생/검증)
- Quality gates: self-loop, cycle, orphan, rev 충돌, 도면-품번 링크 불일치 등  
- (P1+) Synthesis: “BOM closure(전개 결과 캐시)”, “영향도 캐시”, “rev diff snapshot” 등 파생 데이터 생성

### Query(조회)
- FastAPI Query endpoints: BOM tree, where-used(영향도), rev compare, drawing lookup  
- (P1+) 결과 캐시(예: Redis) + pagination/limit/depth guard

## 비동기/큐/재처리/재시도 권장안

- **FastAPI `BackgroundTasks`는 “짧고 가벼운 후처리”에만**: 응답 반환 후 실행 가능하나, 무거운 작업/분산 재시도/모니터링이 필요한 경우 작업큐를 쓰는 게 일반적입니다. ([fastapi.tiangolo.com](https://fastapi.tiangolo.com/tutorial/background-tasks/?utm_source=chatgpt.com))  
- **Celery(권장) + Redis/RabbitMQ**
  - 장점: 분산 워커, 재시도, 백오프, 모니터링(Flower 등), task routing  
  - 재시도: `retry_backoff`로 exponential backoff 적용 가능(1s,2s,4s…) ([docs.celeryq.dev](https://docs.celeryq.dev/en/main/userguide/tasks.html))  
- **재시도(retry) vs 재처리(reprocess) 분리**
  - retry: 네트워크/DB 잠깐 장애 등 “일시적” 실패만 자동  
  - reprocess: 템플릿 미지원/저신뢰 OCR/규칙 충돌 등 “결정이 필요한” 실패는 `NEEDS_REVIEW`로 보내고 HITL에서 재처리 버튼으로 재실행  
- **Idempotency 키**
  - `file_fingerprint = sha256(file_bytes)`  
  - `ingest_key = tenant_id + project_id + source_type + file_fingerprint`  
  - ingestion 단계에서 동일 키면 같은 job 반환(또는 새 revision으로 저장하되 동일 job group에 묶기)
