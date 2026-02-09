# CLAUDE.md — Fabbit Server (Ontology Engine)

## 프로젝트 개요

제조업 온톨로지 기반 데이터 파이프라인 서버. Excel/CSV 파일을 업로드하면 LLM이 구조를 분석(1회)하고, 순수 Python으로 Apache AGE 그래프 DB에 배치 적재합니다.

## 기술 스택

- Python 3.12+, FastAPI, Pydantic
- PostgreSQL + Apache AGE (그래프 DB 확장)
- SQLAlchemy (ORM + 세션 관리), Alembic (마이그레이션)
- LangChain + LangChain-OpenAI (LLM 추상화), 기본 모델: gpt-5-mini
- pandas, openpyxl (Excel/CSV 파싱)
- 패키지 관리: uv

## 주요 명령어

```bash
# 인프라 실행
docker compose up -d

# 의존성 설치
uv sync

# 서버 실행
uv run uvicorn app.main:app --reload

# 시드 데이터 삽입
uv run python seed_data.py

# DB 초기화 (데이터 포함 재생성)
docker compose down -v && docker compose up -d
```

## 프로젝트 구조

```
app/
├── main.py                          # FastAPI 앱 엔트리포인트
├── api/
│   ├── deps.py                      # 공통 Dependency (인증 등, 현재 빈 파일)
│   └── v1/
│       └── ontology.py              # 전체 API 엔드포인트 라우터
├── core/
│   └── config.py                    # Pydantic Settings (환경변수)
├── infrastructure/
│   ├── age_client.py                # Apache AGE 커넥션 관리 + Cypher 실행
│   └── llm_client.py               # OpenAI API 추상화
└── modules/
    └── ontology/
        ├── constants.py             # 온톨로지 스키마 정의 (Single Source of Truth)
        ├── schemas.py               # Pydantic 요청/응답 모델
        ├── service.py               # 비즈니스 로직 (매핑, 인제스션, 질의)
        └── repository.py            # 데이터 접근 (Cypher 생성, SQL 실행)
```

## 아키텍처 패턴

**Modular Layered Architecture (Service-Repository 패턴)**:
- `api/` → HTTP 요청 처리, 파일 파싱
- `modules/service` → 비즈니스 로직 (매핑 생성/검증, 배치 인제스션 오케스트레이션, 질의)
- `modules/repository` → 데이터 접근 (Cypher 생성, 값 포맷팅, SQL CRUD)
- `infrastructure/` → 외부 시스템 (AGE 커넥션, OpenAI API)

## 핵심 도메인 개념

### 온톨로지 노드
- **Part** — 부품 (merge key: `part_number`)
- **Material** — 재질 (merge key: `name`)
- **Supplier** — 공급업체 (merge key: `name`)
- **Drawing** — 도면 (merge key: `drawing_number`)

### 관계 타입
- `CONSISTS_OF` (Part → Part), `MADE_OF` (Part → Material)
- `SUPPLIED_BY` (Part → Supplier), `DEFINED_BY` (Part → Drawing)

### 테넌트 격리 (Schema-per-Tenant)
- `_org_id` 노드 속성 방식은 **폐기 예정**. 모든 격리는 schema-per-tenant로 전환
- `public` 스키마: 전역 공유 데이터 (organizations, users, subscriptions)
- `tenant_{org_id}` 스키마: 테넌트별 비즈니스 데이터 + AGE 그래프
- 요청 시 `SET search_path TO tenant_{org_id}, public`으로 스키마 전환

### 확장 속성
- 온톨로지에 없는 컬럼은 `_ext_` 프리픽스로 노드 속성에 저장
- 예: `탄소배출량` → `_ext_carbon_emission`

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/pipeline/mapping/preview` | Excel 업로드 → LLM 매핑 미리보기 |
| POST | `/pipeline/mapping/confirm` | 매핑 확정 및 DB 저장 |
| GET | `/pipeline/mappings` | 저장된 매핑 목록 |
| POST | `/pipeline/ingest` | Excel + mapping_id → 배치 적재 |
| POST | `/pipeline/query` | 자연어 질의 (테넌트 격리) |
| POST | `/ontology/cypher` | 자연어 → Cypher 변환 및 실행 |
| GET | `/health` | 헬스체크 |

## 데이터 파이프라인 흐름

1. **매핑 미리보기**: Excel 업로드 → LLM이 헤더+샘플 분석 → 매핑 JSON 반환
2. **매핑 확정**: 사용자 검토 후 확정 → `column_mappings` 테이블 저장 (Human-in-the-loop)
3. **배치 인제스션**: Excel + mapping_id → 500행 청크 → 노드 MERGE → 관계 MERGE → 커밋
4. **자연어 질의**: 질문 → LLM이 Cypher 생성 (테넌트 격리 포함) → 실행 → 결과 반환

## 데이터베이스 & ORM 가이드라인

### DB 접근 원칙
1. **SQLAlchemy 우선**: 모든 DB 접근은 SQLAlchemy Session을 통해야 함 (psycopg2 직접 사용 금지)
2. **ORM for 정적 테이블**: `public` 스키마 테이블(Users, Orgs 등)은 SQLAlchemy 모델로 매핑
3. **Raw SQL for AGE**: Cypher 쿼리는 `sqlalchemy.text()`로 실행. AGE vertex를 ORM 객체로 매핑하지 않음
4. **테넌트 격리**: 반드시 `SET search_path` 또는 명시적 그래프 이름으로 데이터 격리 보장

### 스키마 설계

#### `public` 스키마 (Global Shared)
- 전체 서비스 운영을 위한 마스터 데이터
- 주요 테이블: `organizations` (id, name, plan_type), `users` (id, email, current_org_id), `subscriptions` (org_id, status, ai_credits_balance)
- Alembic 표준 마이그레이션 사용

#### `tenant_{org_id}` 스키마 (Isolated)
- 특정 고객사만의 비즈니스 데이터 및 지식 그래프
- SQL 테이블: projects, schedules, folders, file_metadata, column_mappings
- AGE 그래프: 스키마 내 독립 그래프로 존재
- Alembic Template 기반 + Custom Migration Runner

### 테넌트 세션 관리 (FastAPI Depends)

```python
# app/api/deps.py 구현 방향
def get_tenant_db(org_id: str = Depends(get_current_org_id)):
    db = SessionLocal()
    try:
        schema_name = f"tenant_{org_id}"
        db.execute(text(f"SET search_path TO {schema_name}, public"))
        yield db
    finally:
        db.close()
```

### 마이그레이션 전략 (Alembic)
- **public 트랙**: 일반 `alembic upgrade head`로 public 스키마만 관리
- **tenant 트랙**: 모든 테넌트 스키마를 순회하며 동일 구조 적용
  - `env.py`에서 `include_schemas=True` + `tenant_*` 패턴 필터링
  - Migration Runner가 organizations 테이블 조회 후 각 스키마에 적용

### 테넌트 프로비저닝 (신규 조직 가입 시)
1. `tenant_{org_id}` 스키마 내 AGE 그래프 생성: `SELECT create_graph('tenant_{org_id}')`
2. 기본 테이블 생성 (SQL 템플릿 실행)
3. AGE 인덱스 설정: `SELECT create_vlabel_index('tenant_{org_id}', 'Part', 'part_number')`

### 쿼리 작성 규칙
- 일반 SQL: 스키마명 생략 가능 (`SET search_path` 덕분)
- AGE Cypher: 반드시 첫 번째 인자에 **동적 그래프 이름** 사용
  - `SELECT * FROM cypher(:graph_name, $$ ... $$)`
- 테넌트 격리 테스트: 서로 다른 org_id 세션이 상대 데이터를 볼 수 없는지 반드시 검증

### 데이터 백업
- 전체 DB: 주기적 백업
- 특정 테넌트: `pg_dump -n tenant_{org_id}`로 스키마 단위 추출

## 주의사항

- `apache-age-python` 패키지는 사용하지 않음 (빌드 이슈). `_setup_age()` 함수로 직접 초기화
- Apache AGE Cypher의 RETURN 컬럼 수를 자동 파싱하여 SQL 래핑 (`_count_return_columns`)
- CSV 파일은 UTF-8, UTF-16, CP949, EUC-KR 인코딩과 쉼표/탭/세미콜론 구분자를 자동 감지
- 현재 인증 없이 `org_id`를 파라미터로 받음 (임시, Clerk 도입 예정)
