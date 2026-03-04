.PHONY: dev-start dev-db-reset openapi test test-e2e test-e2e-llm test2-unit test2-e2e test2-e2e-llm test2-llm-eval migrate-public migrate-tenant migrate-all revision-public revision-tenant lint

# 개발환경 디비 시작
dev-db-start:
	docker compose -f docker/docker-compose.dev.yml up -d 
	@echo "PostgreSQL 준비 대기..."
	@until docker exec fabbit-db pg_isready -U fabbit -q 2>/dev/null; do sleep 0.5; done
	@echo "PostgreSQL 준비 완료"

# 개발환경 디비 종료
dev-db-stop:
	docker compose -f docker/docker-compose.dev.yml down

# DB 초기화 (볼륨 삭제)
dev-db-reset:
	docker compose -f docker/docker-compose.dev.yml down -v
	@echo "DB 볼륨 삭제 완료."
	@find alembic/versions -type f -name "*.py" -delete
	@find alembic_tenant/versions -type f -name "*.py" -delete
	@echo "마이그래이션 삭제 완료."

# 개발환경 시작 (PostgreSQL + API 서버)
dev-start:
	$(MAKE) dev-db-start
	$(MAKE) migrate-all
	uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

dev-reset:
	$(MAKE) dev-db-reset
	$(MAKE) dev-start

dev-alembic-up:
	$(MAKE) revision-all
	$(MAKE) migrate-all

# OpenAPI 스펙 파일 생성
openapi:
	@curl -s http://localhost:8000/openapi.json | python3 -m json.tool > openapi.json
	@echo "openapi.json 생성 완료"
	@cp ./openapi.json ../web/openapi.json
	@echo "openapi.json 복사 완료"



# ── 마이그레이션 ──



# public revision 자동 생성 (사용: make revision-public m="설명")
revision-public:
	uv run alembic revision --autogenerate -m "$(m)"
	@echo "public 마이그레이션 생성 완료"

# tenant revision 자동 생성 (사용: make revision-tenant m="설명")
revision-tenant:
	uv run alembic -c alembic_tenant.ini revision --autogenerate -m "$(m)"
	@echo "tenant 마이그레이션 생성 완료"

revision-all:
	$(MAKE) revision-public
	$(MAKE) revision-tenant


# public 마이그레이션 적용
migrate-public:
	$(MAKE) revision-public
	uv run alembic upgrade head
	@echo "public 마이그레이션 완료"

# tenant 마이그레이션 적용 (모든 tenant_* 스키마 순회)
migrate-tenant:
	$(MAKE) revision-tenant
	uv run alembic -c alembic_tenant.ini upgrade head
	@echo "tenant 마이그레이션 완료"

# public + tenant 마이그레이션 적용
migrate-all:
	$(MAKE) migrate-public
	$(MAKE) migrate-tenant

# ── 테스트 ──

test:
	uv run pytest tests/ -x

# 통합 테스트 — fixture 매핑 (LLM 없이, 빠름)
test-e2e:
	uv run pytest tests/integration/test_crud_flow.py -v

# 통합 테스트 — 실제 LLM 호출 포함 (매핑 미리보기, AI 질의)
test-e2e-llm:
	uv run pytest tests/integration/test_crud_flow.py -v -s --use-llm

# test2 단위 테스트
test2-unit:
	uv run pytest test2/unit -c test2/pytest.ini -q

# test2 e2e (기본: LLM API 케이스 skip)
test2-e2e:
	uv run pytest test2/e2e -c test2/pytest.ini -v

# test2 e2e (LLM API 케이스 포함)
test2-e2e-llm:
	uv run pytest test2/e2e -c test2/pytest.ini -v --use-llm

# test2 LLM 평가(비용/시간 큼)
test2-llm-eval:
	uv run pytest test2/llm -c test2/pytest.ini -v -s --use-llm --llm-runs=3 -m \"eval and costly\"

# ── 린트 ──

# 아키텍처 규칙 검증 (DB 불필요)
lint:
	uv run ruff check .
	uv run pytest linter/ --confcutdir=linter -o "python_files=check_*.py" -o "python_functions=check_*" --tb=line --no-header -q
