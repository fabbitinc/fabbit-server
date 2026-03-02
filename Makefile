.PHONY: dev-start dev-stop dev-db-reset openapi test test-e2e test-e2e-llm migrate-public migrate-tenant migrate-all revision-public revision-tenant lint

# 개발환경 디비 시작
dev-db-start:
	docker compose -f docker-compose.dev.yml up -d 
	@echo "PostgreSQL 준비 대기..."
	@until docker exec fabbit-db pg_isready -U fabbit -q 2>/dev/null; do sleep 0.5; done
	@echo "PostgreSQL 준비 완료"

# 개발환경 디비 종료
dev-db-stop:
	docker compose -f docker-compose.dev.yml down

# DB 초기화 (볼륨 삭제)
dev-db-reset:
	docker compose -f docker-compose.dev.yml down -v
	@echo "DB 볼륨 삭제 완료."
	@find alembic/versions -type f -name "*.py" -delete
	@find alembic_tenant/versions -type f -name "*.py" -delete
	@echo "마이그래이션 삭제 완료."
	$(MAKE) dev-db-start
	@nohup uv run uvicorn app.main:app --host 0.0.0.0 --port 8000 > uvicorn.log 2>&1 & echo $$! > uvicorn.pid
	@echo "서버 응답 대기 중 (http://localhost:8000/health)..."
	@for i in $$(seq 1 30); do \
		if curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/health | grep -q "200"; then \
			echo "\n서버 준비 완료!"; \
			break; \
		fi; \
		echo -n "."; \
		sleep 1; \
		if [ $$i -eq 30 ]; then \
			echo "\n서버 시작 시간 초과!"; \
			$(MAKE) dev-db-cleanup; \
			exit 1; \
		fi; \
	done
	$(MAKE) revision-all
	-@kill $$(cat uvicorn.pid) 2>/dev/null || true
	-@rm uvicorn.pid uvicorn.log 2>/dev/null || true
	@echo "DB 리셋 및 마이그레이션 생성 완료."

# 개발환경 시작 (PostgreSQL + API 서버)
dev-start:
	$(MAKE) dev-db-start
	$(MAKE) migrate-all
	uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

dev-reset:
	$(MAKE) dev-stop
	$(MAKE) dev-db-reset
	$(MAKE) dev-start

# OpenAPI 스펙 파일 생성
openapi:
	@curl -s http://localhost:8000/openapi.json | python3 -m json.tool > openapi.json
	@echo "openapi.json 생성 완료"
	@cp ./openapi.json ../web/openapi.json
	@echo "openapi.json 복사 완료"



# ── 마이그레이션 ──

# public 마이그레이션 적용
migrate-public:
	uv run alembic upgrade head

# tenant 마이그레이션 적용 (모든 tenant_* 스키마 순회)
migrate-tenant:
	uv run alembic -c alembic_tenant.ini upgrade head

# public + tenant 마이그레이션 적용
migrate-all:
	uv run alembic upgrade head
	uv run alembic -c alembic_tenant.ini upgrade head
	@echo "마이그레이션 완료"

# public revision 자동 생성 (사용: make revision-public m="설명")
revision-public:
	uv run alembic revision --autogenerate -m "$(m)"

# tenant revision 자동 생성 (사용: make revision-tenant m="설명")
revision-tenant:
	uv run alembic -c alembic_tenant.ini revision --autogenerate -m "$(m)"

revision-all:
	uv run alembic revision --autogenerate -m "$(m)"
	uv run alembic -c alembic_tenant.ini revision --autogenerate -m "$(m)"
	@echo "마이그레이션 파일 생성 완료"


# ── 테스트 ──

test:
	uv run pytest tests/ -x

# 통합 테스트 — fixture 매핑 (LLM 없이, 빠름)
test-e2e:
	uv run pytest tests/integration/test_crud_flow.py -v

# 통합 테스트 — 실제 LLM 호출 포함 (매핑 미리보기, AI 질의)
test-e2e-llm:
	uv run pytest tests/integration/test_crud_flow.py -v -s --use-llm

# ── 린트 ──

# 아키텍처 규칙 검증 (DB 불필요)
lint:
	uv run ruff check .
	uv run pytest linter/ --confcutdir=linter -o "python_files=check_*.py" -o "python_functions=check_*" --tb=line --no-header -q