# Fabbit Server

제조업 온톨로지 기반 데이터 파이프라인 서버. Excel/CSV → LLM 구조 분석(1회) → Apache AGE 그래프 DB 배치 적재.

## 기술 스택

| 기술                                       | 용도                                |
| ------------------------------------------ | ----------------------------------- |
| FastAPI + Pydantic                         | API 서버, 요청/응답 검증            |
| PostgreSQL + Apache AGE                    | 관계형 DB + 그래프 DB (Cypher 쿼리) |
| SQLAlchemy + Alembic                       | ORM, 세션 관리, 스키마 마이그레이션 |
| LangChain + OpenRouter (openai/gpt-5-mini) | LLM 추상화 (매핑 분석, 자연어 질의) |
| pandas + openpyxl                          | Excel/CSV 파싱                      |
| uv                                         | 패키지 관리                         |

## 아키텍처

**Service-Repository 패턴**:

- `api/` → HTTP 요청 처리, 파일 파싱
- `modules/*/service.py` → 비즈니스 로직 오케스트레이션
- `modules/*/repository.py` → 데이터 접근 (SQL, Cypher)
- `infrastructure/` → 외부 시스템 (AGE, OpenAI)

## 도메인 개념

**SSoT**: `app/modules/ontology/base_ontology.py` — 노드/관계/속성의 단일 진실 공급원. LLM 프롬프트, 매핑 검증, 인제스션, 인덱스 생성에 모두 이 정의를 참조

**온톨로지 노드**: Part(`part_number`), Drawing(`drawing_number`), Supplier(`company_name`), Project(`name`)
**노드 dual-write**: Part, Drawing, Supplier는 RDS에 전체 속성 + Graph에 merge key만 유지
**관계**:

- `CONSISTS_OF`(Part→Part) — RDS `bom_links` + Graph
- `HAS_ITEM`(Project→Part) — RDS `project_parts` + Graph
- `DEFINED_BY`(Part→Drawing) — RDS `parts.drawing_id` FK + Graph (N:1)
- `SUPPLIED_BY`(Part→Supplier) — RDS `part_suppliers` + Graph (M:N)
  **확장 속성**: 온톨로지에 없는 컬럼은 `_ext_` 프리픽스로 노드 속성 저장 (JSONB `extended_properties`)

## 주요 스킬

- rotuer 작성시 사용 스킬: api-guide
- service, quires, use_cases 작성시 사용 스킬: business-layer-guide
- model 작성시 사용 스킬: models-guide

## 서브 스킬

- 데이터베이스 설정시 사용 스킬: database-guide
- 로깅 작성시 사용 스킬:logging-guide

## 마이그레이션

- **사용자의 별도 지시없이 마이그레이션은 고려하지 않습니다.**
