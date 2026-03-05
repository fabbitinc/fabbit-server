.PHONY: dev-start dev-db-reset openapi test test-e2e test-e2e-llm test2-unit test2-e2e test2-e2e-external test2-llm-eval karate-generate-openapi karate-test karate-test-auth karate-test-flow-project playwright-test-all-api playwright-test-excluded-api karate-test-all-api-dry karate-test-docker karate-test-auth-docker migrate-public migrate-tenant migrate-all revision-public revision-tenant lint

# 개발환경 디비 종료
dev-db-stop:
	docker compose -f docker/docker-compose.dev.yml down

# 개발환경 디비 시작
dev-db-start:
	docker compose -f docker/docker-compose.dev.yml up -d 
	@echo "PostgreSQL 준비 대기..."
	@until docker exec fabbit-db pg_isready -U fabbit -q 2>/dev/null; do sleep 0.5; done
	@echo "PostgreSQL 준비 완료"
	$(MAKE) migrate-all

# DB 초기화 (볼륨 삭제 + 마이그레이션 파일 초기화)
dev-db-reset:
	docker compose -f docker/docker-compose.dev.yml down -v
	@echo "DB 볼륨 삭제 완료."
	@rm -f migrations/public/*.sql migrations/public/atlas.sum
	@rm -f migrations/tenant/*.sql migrations/tenant/atlas.sum
	@echo "마이그레이션 파일 삭제 완료."

dev-db-restart:
	$(MAKE) dev-db-reset
	$(MAKE) dev-db-start


# OpenAPI 스펙 파일 생성
openapi:
	@curl -s http://localhost:8080/openapi.json | python3 -m json.tool > openapi.json
	@echo "openapi.json 생성 완료"
	@cp ./openapi.json ../web/openapi.json
	@echo "openapi.json 복사 완료"



# ── 마이그레이션 ──



# public revision 자동 생성 (사용: make revision-public m="설명")
revision-public:
	atlas migrate diff --env public -c "file://migrations/atlas.hcl"
	@echo "public 마이그레이션 생성 완료"

# tenant revision 자동 생성 (사용: make revision-tenant m="설명")
revision-tenant:
	atlas migrate diff --env tenant -c "file://migrations/atlas.hcl"
	@echo "tenant 마이그레이션 생성 완료"

# public 마이그레이션 적용
migrate-public:
	$(MAKE) revision-public
	atlas migrate apply --env public -c "file://migrations/atlas.hcl"
	@echo "public 마이그레이션 완료"

# tenant 마이그레이션 적용 (모든 tenant_* 스키마 순회)
migrate-tenant:
	$(MAKE) revision-tenant
	@SCHEMAS=$$(docker exec fabbit-db psql -U fabbit -t -A -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%'"); \
	if [ -z "$$SCHEMAS" ]; then \
		echo "적용할 tenant 스키마 없음 (skip)"; \
	else \
		for s in $$SCHEMAS; do \
			echo "tenant 마이그레이션 적용: $$s"; \
			atlas migrate apply --dir "file://migrations/tenant" --dev-url "docker://postgres/18/dev?search_path=public" --url "postgres://fabbit:fabbit@localhost:5432/fabbit?search_path=$$s&sslmode=disable"; \
		done; \
		echo "tenant 마이그레이션 완료"; \
	fi

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

# test2 e2e external (실외부 호출)
test2-e2e-external:
	uv run pytest test2/e2e/external -c test2/pytest.ini -v --use-llm -m "e2e and external and costly"

# test2 LLM 평가(비용/시간 큼)
test2-llm-eval:
	uv run pytest test2/llm_eval -c test2/pytest.ini -v -s --use-llm --llm-runs=3 -m "llm_eval and costly"

# Playwright 전체 API 검증 실행 (OpenAPI contracts + 통합/보안/멱등/실패/스트레스)
playwright-test-all-api:
	cd playwright && npm ci
	cd playwright && npm run generate:matrix
	cd playwright && npm run check:coverage
	cd playwright && npm run test:contracts
	cd playwright && npm run test:flows
	cd playwright && npm run test:security
	cd playwright && npm run test:idempotency
	cd playwright && npm run test:failure
	cd playwright && npm run test:stress

# Playwright 제외 API 선택 실행 (기본 미실행)
playwright-test-excluded-api:
	cd playwright && npm ci
	cd playwright && npm run test:excluded

# ── 린트 ──

# 아키텍처 규칙 검증 (DB 불필요)
lint:
	uv run ruff check .
	uv run pytest linter/ --confcutdir=linter -o "python_files=check_*.py" -o "python_functions=check_*" --tb=line --no-header -q
