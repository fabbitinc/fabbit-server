# A. 핵심 결론 10줄 요약

1) MVP는 “모듈러 모놀리스(FastAPI) + 작업큐(워커) + Postgres(관계형+AGE)”로 시작하는 게 가장 빠르고 안전합니다.  
2) 원본(BOM XLSX/CSV, 도면 PDF/DXF)은 객체스토리지에 버저닝/불변 보관하고, job_id로 모든 산출물을 연결해 재현성을 확보합니다. ([docs.aws.amazon.com](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html))  
3) 파싱/매핑은 rule-first로 설계하고, 템플릿 미인식/저신뢰 구간에서만 LLM fallback을 호출해 비용을 통제합니다.  
4) 그래프 적재는 AGE의 `MERGE`를 기본 upsert primitive로 쓰고, 부분 매치가 필요하면 여러 `MERGE`로 분해하는 방식을 표준화합니다. ([age.apache.org](https://age.apache.org/age-manual/master/clauses/merge.html))  
5) 온톨로지는 “정규화된 canonical(중간) 모델 → 그래프 projection” 구조로 만들면 재처리/스키마 변경/감사 대응이 쉬워집니다.  
6) AGE는 기본 인덱스를 자동 생성하지 않으니, `id/start_id/end_id` BTREE + `properties` GIN + (자주 쓰는 key) BTREE(agtype_access_operator) 조합을 P0부터 강제합니다. ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  
7) 멀티테넌시는 P0에서는 tenant당 graph(=Postgres namespace) 분리가 가장 단순·안전하며, AGE는 graph 생성 시 namespace를 만든다는 점을 활용합니다. ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  
8) 품질 게이트(사이클/self-loop/orphan/수량·단위 이상/도면-품번 불일치)를 자동 검증으로 고정하고, 실패 시 “재시도”가 아니라 “재처리/리뷰”로 분기합니다.  
9) LangChain 구간은 structured output(JSON/Pydantic) + 캐시 + fallback 체인으로 고정해 오류/비용을 줄입니다. ([docs.langchain.com](https://docs.langchain.com/oss/python/langchain/structured-output))  
10) 운영 관측성은 OpenTelemetry로 API↔워커↔DB까지 trace를 연결하고, KPI(파싱 성공률/매핑 정확도/처리시간/재처리율/LLM 호출수)를 지표화합니다. ([opentelemetry-python-contrib.readthedocs.io](https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/fastapi/fastapi.html))  
