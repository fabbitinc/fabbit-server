# F. 의사결정이 필요한 쟁점(선택지 A/B + 추천안)

## 1) 멀티테넌시 격리 방식
- A) **Graph-per-tenant(tenant별 graph/schema)**  
  - 장점: 격리 단순, 실수 방지, 삭제/이관 쉬움  
  - 단점: tenant 증가 시 DDL/인덱스/운영 부담(추정)  
- B) **Single graph + tenant_id + (RLS/필터링)**  
  - 장점: 중앙관리/통합 분석 쉬움  
  - 단점: 필터 누락/보안 실수 리스크, RLS+함수 보안까지 설계 복잡  
- **추천: P0는 A, P1에서 tenant 수/요구사항 보고 B 검토** ([age.apache.org](https://age.apache.org/age-manual/master/intro/graphs.html))  

## 2) Revision 모델(Part vs PartRev)
- A) **PartRev 노드 분리 + BOM은 PartRev→PartRev**  
  - 장점: rev 비교/추적/검증이 명확  
  - 단점: 노드 수 증가  
- B) Part만 두고 rev를 property로 갱신  
  - 장점: 단순  
  - 단점: rev diff/이력/감사 거의 불가능  
- **추천: A(PartRev 분리)**

## 3) BOM edge의 유일성 기준
- A) (parent, child) 단위로 edge 1개  
  - 단점: 중복 라인(find_no/refdes) 덮어씀  
- B) **row_key(라인 식별자) 기반 edge**  
  - 장점: 라인 단위 추적/감사/재처리 가능  
- **추천: B**

## 4) 비동기 처리 프레임워크
- A) FastAPI `BackgroundTasks` 중심  
  - 장점: 도입 쉬움  
  - 단점: 무거운 작업/분산 재시도/모니터링에 부적합  
- B) **Celery(또는 동급) 작업큐**  
  - 장점: 분산 워커/재시도/백오프/모니터링  
- **추천: B** ([fastapi.tiangolo.com](https://fastapi.tiangolo.com/tutorial/background-tasks/?utm_source=chatgpt.com))  

## 5) PDF 처리 전략
- A) **native text 우선(PyMuPDF) + OCR fallback**  
  - 장점: 비용/지연 최소, 정확도 높을 때 많음  
- B) 전량 OCR  
  - 장점: 파이프라인 단순해 보임  
  - 단점: 비용/지연 증가, 품질 편차 큼  
- **추천: A** ([pymupdf.readthedocs.io](https://pymupdf.readthedocs.io/en/latest/recipes-text.html?utm_source=chatgpt.com))  

## 6) LLM 적용 범위
- A) **rule-first + fallback 트리거 엄격**  
  - 장점: 재현성/원가/운영 안정성  
- B) LLM-first(대부분 LLM)  
  - 장점: 초기 개발 빠를 수 있음  
  - 단점: 비용/변동성/감사 어려움  
- **추천: A** ([docs.langchain.com](https://docs.langchain.com/oss/python/langchain/structured-output))  

## 7) “rev diff/영향도” 계산 위치
- A) Cypher에서 집계/정렬까지 모두 처리  
  - 리스크: 일부 케이스에서 성능 이슈 리포트(추정)  
- B) **Cypher는 raw traversal만, diff/리포트는 앱/SQL/캐시에서**  
- **추천: P0~P1은 B** ([github.com](https://github.com/apache/age/issues/2194))  
