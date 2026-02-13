# E. 위험요소 TOP10 + 대응

1) **BOM 템플릿 변이 폭발(컬럼명/순서/병합셀/다중 헤더)**  
- 대응: 템플릿 레지스트리 + 헤더 자동탐지 + 온보딩 워크플로우(P1) + unknown은 NEEDS_REVIEW

2) **도면 PDF가 스캔본(텍스트 없음) → OCR 품질 편차**  
- 대응: native text 우선(PyMuPDF), OCR은 fallback, TSV confidence + anchor rule로 신뢰도 평가 ([pymupdf.readthedocs.io](https://pymupdf.readthedocs.io/en/latest/app1.html?utm_source=chatgpt.com))  

3) **Tesseract confidence를 맹신(높아도 틀릴 수 있음)**  
- 대응: confidence는 “보조 지표”로만, 반드시 규칙 기반 anchor/형식검증 병행 ([aclanthology.org](https://aclanthology.org/2023.nlp4dh-1.20.pdf))  

4) **그래프 중복 적재(같은 BOM 라인/도면 rev 반복 업로드)**  
- 대응: row_key 기반 edge MERGE, file_hash 기반 idempotency, job 단계 체크포인트

5) **BOM 사이클/자기참조(self-loop)로 조회/전개가 무한 또는 폭발**  
- 대응: ingest 시 cycle/self-loop 게이트, depth 제한, 실패 시 리뷰

6) **AGE 인덱스 미사용/성능 문제(일부 패턴에서 보고됨)**  
- 대응: P0부터 `EXPLAIN`(Cypher 내부)로 검증 루틴, 인덱스/쿼리 형태 컨벤션 고정 ([learn.microsoft.com](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance))  

7) **Aggregation/Ordering 기반 쿼리가 Cypher에서 느릴 수 있음(추정)**  
- 대응: 고급 diff/리포트는 애플리케이션/SQL로 계산하거나 파생 테이블/캐시로 분리 ([github.com](https://github.com/apache/age/issues/2194))  

8) **멀티테넌시 격리 실패(권한/버그/쿼리 누락)**  
- 대응: P0는 graph-per-tenant + API 레벨 강제, P1에서 RLS 도입 시 보안 속성까지 리뷰 ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  

9) **LLM 비용/지연이 운영 비용을 잠식**  
- 대응: trigger를 명확히(unknown template, low-confidence link 등), structured output + caching ([docs.langchain.com](https://docs.langchain.com/oss/python/langchain/structured-output))  

10) **감사/재현 불가능(“왜 이렇게 매핑됐는지” 설명 못함)**  
- 대응: 원본/중간 산출물/템플릿 버전/룰 버전/LLM 입력·출력(또는 해시) 저장 + 객체스토리지 버저닝 ([docs.aws.amazon.com](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html))  
