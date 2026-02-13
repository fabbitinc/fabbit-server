# D. 구현 체크리스트(P0/P1/P2)

## P0 (즉시, MVP 필수)

### 아키텍처/파이프라인
- [ ] 업로드 API: `POST /projects/{id}/imports` (BOM/PDF/DXF), 응답은 `job_id`
- [ ] 객체스토리지 저장 + `file_hash` 계산 + (가능하면) 버저닝 활성화 ([docs.aws.amazon.com](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html))  
- [ ] `ingestion_job` 상태머신(최소): `QUEUED → RUNNING → SUCCEEDED | NEEDS_REVIEW | FAILED`
- [ ] Celery(또는 동급) 작업큐 도입, retry_backoff 적용 ([docs.celeryq.dev](https://docs.celeryq.dev/en/main/userguide/tasks.html))  
- [ ] 단계별 아티팩트 저장: `raw`, `parsed`, `normalized`, `mapping_plan`, `validation_report`

### BOM 파싱
- [ ] 템플릿 감지(헤더 행 자동 탐지) + 컬럼 매핑(synonym dict)
- [ ] canonical BOM row 스키마 정의(필수 필드: parent/child/qty/uom/rev/line_no/source_row)
- [ ] 단위/수량/rev 정규화 룰(P0는 EA/PCS/SET 정도만)
- [ ] 실패/저신뢰는 `NEEDS_REVIEW`로 보내고 원인코드 저장

### PDF/DXF 파싱
- [ ] PDF: PyMuPDF 기반 native text 우선(`page.get_text("text"/"words")`) ([pymupdf.readthedocs.io](https://pymupdf.readthedocs.io/en/latest/recipes-text.html?utm_source=chatgpt.com))  
- [ ] PDF: 타이틀블록 후보 영역 추출(룰) + 텍스트 적으면 OCR fallback(Tesseract TSV) ([tesseract-ocr.github.io](https://tesseract-ocr.github.io/tessdoc/Command-Line-Usage.html))  
- [ ] DXF: ezdxf로 `TEXT/MTEXT` 및 블록에서 텍스트 추출 ([ezdxf.readthedocs.io](https://ezdxf.readthedocs.io/?utm_source=chatgpt.com))  

### Apache AGE
- [ ] tenant당 graph 생성(네임스페이스 분리) ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  
- [ ] 라벨(Part/PartRev/Drawing/DrawingRev/BOM_ITEM) 생성(또는 Cypher CREATE로 자동) ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  
- [ ] 필수 인덱스(id/start_id/end_id/properties + key targeted index) ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  
- [ ] 업서트는 `MERGE` 기반으로 구현 ([age.apache.org](https://age.apache.org/age-manual/master/clauses/merge.html))  

### 품질 게이트(자동 검증)
- [ ] self-loop 검사
- [ ] cycle 검사
- [ ] orphan(루트/프로젝트 미연결) 검사
- [ ] qty <=0, uom null 검사
- [ ] 도면-품번 링크 누락률 측정

### 관측성/운영
- [ ] OpenTelemetry FastAPI instrumentation 도입 ([opentelemetry-python-contrib.readthedocs.io](https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/fastapi/fastapi.html))  
- [ ] KPI 최소 6개: 파싱 성공률/매핑 성공률/처리시간/재시도율/NEEDS_REVIEW 비율/LLM 호출수

### P0 DoD(완료 기준)
- [ ] “대표 템플릿 2~3종” BOM을 end-to-end로 적재 후 BOM 트리/where-used 조회 API 제공
- [ ] 동일 파일 재업로드 시 idempotent(중복 노드/엣지 없음)
- [ ] 실패 건은 반드시 원인코드 + 재처리 가능

---

## P1 (단기, 안정화/유료화 직결)

### 템플릿 운영
- [ ] 템플릿 레지스트리(버전/필수컬럼/정규화 룰) DB화
- [ ] “신규 템플릿 학습(온보딩) → 승인 → 재사용” 워크플로우 UI/HITL
- [ ] 예외 처리 번들(원본, 샘플 row, 자동 추정 매핑, 에러 원인) 자동 생성

### 증분(diff) 처리
- [ ] PDF: page 단위 fingerprint 저장(텍스트/렌더 기반) 후 변경 페이지만 재처리
- [ ] DXF: entity 텍스트 fingerprint(정렬된 (type,text,layer,point) 해시) 기반 증분
- [ ] Graph: DrawingRev/PartRev “신규 revision 생성 + 이전 rev superseded” 정책 고정

### 쿼리 성능
- [ ] 자주 쓰는 패턴에 대한 인덱스 추가/검증(EXPLAIN 루틴) ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  
- [ ] 대형 트리 조회는 “closure 캐시(파생)” 또는 depth 제한 + pagination
- [ ] Rev 비교는 애플리케이션 레벨 diff + 캐시

### 보안
- [ ] 관계형 canonical 테이블에 RLS 적용(tenant_id 기준) ([postgresql.org](https://www.postgresql.org/docs/current/ddl-rowsecurity.html))  
- [ ] 감사로그 확장: 누가/언제/무엇을/어떤 규칙·템플릿·LLM결과로 적재했는지

### 운영
- [ ] 백업/PITR(필요 시) 시나리오 문서화(런북) ([postgresql.org](https://www.postgresql.org/docs/current/continuous-archiving.html))  
- [ ] 알람: queue lag, 실패율 급증, OCR fallback 비율 급증, 인덱스 미사용(EXPLAIN 샘플링)

### P1 DoD
- [ ] 신규 템플릿 온보딩을 “개발자 개입 없이” HITL로 처리 가능
- [ ] 재업로드/수정 업로드 시 증분 처리가 동작
- [ ] 운영 알람 및 런북으로 1차 대응 가능

---

## P2 (중기, 고급 분석/확장)

- [ ] BOM 영향도(상위 조립품/고객 프로젝트 영향) 그래프 분석 고도화
- [ ] Rule DSL(검증 규칙/정규화 규칙) + 룰 실행 이력 관리
- [ ] 도면 의미론(타이틀블록 표준화, 레이어별 의미, 부품 callout 인식)
- [ ] 대체부품(Alternate), 옵션 구성(Variant), 유효기간(Effectivity) 모델링 확장
- [ ] 그래프 기반 추천/유사도(단, 비용/정확도 검증 후)

### P2 DoD
- [ ] “rev diff + 영향도 + 검증 규칙”을 결합한 변경검토 리포트 자동 생성
