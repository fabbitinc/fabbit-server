# G. 참고 근거(공식 문서/신뢰 가능한 출처 링크)

## Apache AGE
- Graph가 Postgres namespace로 저장되고 `create_vlabel()`로 label 테이블이 생성되는 구조  
  - https://age.apache.org/age-manual/master/intro/graphs.html  
- `agtype`가 JSON superset이며 AGE의 기본 반환 타입이라는 설명  
  - https://age.apache.org/age-manual/master/intro/types.html  
- `MERGE` 동작(전체 패턴 매치/생성, 부분 매치 시 여러 MERGE 권장)  
  - https://age.apache.org/age-manual/master/clauses/merge.html  
- `cypher(graph_name, query_string, parameters)` 호출 형식  
  - https://age.apache.org/age-manual/v0.6.0/intro/cypher.html  
- 인덱싱이 Postgres 인덱스를 활용하며 BTree/GIN 등을 지원한다는 FAQ  
  - https://age.apache.org/faq/  
- 인덱싱/EXPLAIN/WHERE vs inline map 차이 및 인덱스 예시(가이드)  
  - https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-performance  

## PostgreSQL(업서트/보안/RLS)
- `INSERT ... ON CONFLICT DO UPDATE`(업서트) 공식 문서  
  - https://www.postgresql.org/docs/current/sql-insert.html  
- Row Level Security(RLS) 공식 문서  
  - https://www.postgresql.org/docs/current/ddl-rowsecurity.html  
- `SECURITY INVOKER/DEFINER` 함수 권한 공식 문서  
  - https://www.postgresql.org/docs/current/sql-createfunction.html  
- PostgreSQL 연속 아카이빙/복구(PITR 관련)  
  - https://www.postgresql.org/docs/current/continuous-archiving.html  

## FastAPI / Celery(비동기/재시도)
- FastAPI Background Tasks 공식 문서  
  - https://fastapi.tiangolo.com/tutorial/background-tasks/  
- Celery retry/backoff 공식 문서  
  - https://docs.celeryq.dev/en/main/userguide/tasks.html  
- FastAPI + Celery 실무 가이드(예시)  
  - https://testdriven.io/blog/fastapi-and-celery/  

## PDF / OCR / DXF 파싱
- PyMuPDF 텍스트 추출  
  - https://pymupdf.readthedocs.io/en/latest/recipes-text.html  
- PyMuPDF `page.find_tables()`  
  - https://pymupdf.readthedocs.io/en/latest/page.html  
- Tesseract TSV 출력(좌표/신뢰도 산출)  
  - https://tesseract-ocr.github.io/tessdoc/Command-Line-Usage.html  
- Tesseract manpage(DPI 등 옵션)  
  - https://manpages.ubuntu.com/manpages/focal/man1/tesseract.1.html  
- OCR confidence를 정확도 보장으로 보기 어렵다는 점(연구/분석)  
  - https://aclanthology.org/2023.nlp4dh-1.20.pdf  
- ezdxf 문서  
  - https://ezdxf.readthedocs.io/  

## LangChain(구조화/캐시/폴백)
- Structured output(JSON/Pydantic)  
  - https://docs.langchain.com/oss/python/langchain/structured-output  
- Cache 레퍼런스  
  - https://reference.langchain.com/v0.3/python/community/cache.html  
- Runnable / fallback 관련 레퍼런스  
  - https://reference.langchain.com/python/langchain_classic/runnables/  

## 관측성/스토리지
- OpenTelemetry FastAPI instrumentation  
  - https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/fastapi/fastapi.html  
- OpenTelemetry Python instrumentation 개요  
  - https://opentelemetry.io/docs/languages/python/instrumentation/  
- S3 Versioning(버전 보관/복구)  
  - https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html  
