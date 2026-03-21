.PHONY: dev-start dev-db-reset openapi test test-e2e test-e2e-llm test2-unit test2-e2e test2-e2e-external test2-llm-eval karate-generate-openapi karate-test karate-test-auth karate-test-flow-project playwright-test-all-api playwright-test-excluded-api karate-test-all-api-dry karate-test-docker karate-test-auth-docker migrate-public migrate-tenant migrate-all revision-public revision-tenant lint format build-3dconverter

THREED_CONVERTER_IMAGE ?= fabbit-3dconverter:latest

# 개발환경 디비 종료
dev-db-stop:
	docker compose -f docker/docker-compose.dev.yml down

# 개발환경 디비 시작
dev-db-start:
	docker compose -f docker/docker-compose.dev.yml up -d 
	@echo "PostgreSQL 준비 대기..."
	@until docker exec fabbit-db pg_isready -U fabbit -q 2>/dev/null; do sleep 0.5; done
	@echo "PostgreSQL 준비 완료"
	$(MAKE) revision-all
	$(MAKE) migrate-all

# DB 초기화 (볼륨 삭제 + 마이그레이션 파일 초기화)
dev-db-reset:
	docker compose -f docker/docker-compose.dev.yml down -v
	@echo "DB 볼륨 삭제 완료."
	@rm -f src/main/resources/migrations/public/[0-9]*.sql
	@rm -f src/main/resources/migrations/tenant/[0-9]*.sql
	@echo "마이그레이션 파일 삭제 완료."

dev-db-restart:
	$(MAKE) dev-db-reset
	$(MAKE) dev-db-start


# OpenAPI 스펙 파일 생성
openapi:
	@curl -s http://localhost:10010/openapi.json | python3 -m json.tool > openapi.json
	@echo "openapi.json 생성 완료"
	@cp ./openapi.json ../web/openapi.json
	@echo "openapi.json 복사 완료"



# ── 마이그레이션 (Liquibase) ──

LB_URL      = jdbc:postgresql://localhost:5432/fabbit
LB_USER     = fabbit
LB_PASS     = fabbit
LB_DEV_PORT = 5433
LB_SEARCH_PATH = src/main/resources
LB_CHANGELOG = migrations/changelog-master.xml
LB = liquibase --username=$(LB_USER) --password=$(LB_PASS) --search-path=$(LB_SEARCH_PATH)

# public diff 자동 생성 (기존 마이그레이션 vs Hibernate DDL)
revision-public:
	@docker rm -f lb-dev-db 2>/dev/null || true
	@docker run --rm -d --name lb-dev-db -e POSTGRES_PASSWORD=dev -p $(LB_DEV_PORT):5432 postgres:18 > /dev/null
	@until docker exec lb-dev-db pg_isready -q 2>/dev/null; do sleep 0.5; done
	@docker exec lb-dev-db psql -U postgres -q -c "CREATE DATABASE current_state"
	@docker exec lb-dev-db psql -U postgres -q -c "CREATE DATABASE desired_state"
	@# current: 기존 마이그레이션 적용
	liquibase --username=postgres --password=dev --search-path=$(LB_SEARCH_PATH) \
		update \
		--url="jdbc:postgresql://localhost:$(LB_DEV_PORT)/current_state" \
		--changelog-file=$(LB_CHANGELOG) \
		--context-filter=public 2>/dev/null || true
	@# desired: Hibernate DDL 적용
	@./gradlew -q schemaExportPublic | docker exec -i lb-dev-db psql -U postgres -d desired_state -q 2>/dev/null
	@# diff: desired vs current (search-path 없이 CWD 기준 출력)
	liquibase --username=postgres --password=dev \
		--url="jdbc:postgresql://localhost:$(LB_DEV_PORT)/current_state" \
		--referenceUrl="jdbc:postgresql://localhost:$(LB_DEV_PORT)/desired_state" \
		--referenceUsername=postgres --referencePassword=dev \
		--changelog-file=src/main/resources/migrations/public/$$(date +%Y%m%d%H%M%S)_diff.sql \
		diffChangeLog || (docker rm -f lb-dev-db > /dev/null 2>&1; exit 1)
	@docker rm -f lb-dev-db > /dev/null
	@echo "public revision 생성 완료"

# tenant diff 자동 생성 (기존 마이그레이션 vs Hibernate DDL)
revision-tenant:
	@docker rm -f lb-dev-db 2>/dev/null || true
	@docker run --rm -d --name lb-dev-db -e POSTGRES_PASSWORD=dev -p $(LB_DEV_PORT):5432 postgres:18 > /dev/null
	@until docker exec lb-dev-db pg_isready -q 2>/dev/null; do sleep 0.5; done
	@docker exec lb-dev-db psql -U postgres -q -c "CREATE DATABASE current_state"
	@docker exec lb-dev-db psql -U postgres -q -c "CREATE DATABASE desired_state"
	@# current: 기존 마이그레이션 적용
	liquibase --username=postgres --password=dev --search-path=$(LB_SEARCH_PATH) \
		update \
		--url="jdbc:postgresql://localhost:$(LB_DEV_PORT)/current_state" \
		--changelog-file=$(LB_CHANGELOG) \
		--context-filter=tenant 2>/dev/null || true
	@# desired: Hibernate DDL 적용
	@./gradlew -q schemaExportTenant | docker exec -i lb-dev-db psql -U postgres -d desired_state -q 2>/dev/null
	@# diff: desired vs current (search-path 없이 CWD 기준 출력)
	liquibase --username=postgres --password=dev \
		--url="jdbc:postgresql://localhost:$(LB_DEV_PORT)/current_state" \
		--referenceUrl="jdbc:postgresql://localhost:$(LB_DEV_PORT)/desired_state" \
		--referenceUsername=postgres --referencePassword=dev \
		--changelog-file=src/main/resources/migrations/tenant/$$(date +%Y%m%d%H%M%S)_diff.sql \
		diffChangeLog || (docker rm -f lb-dev-db > /dev/null 2>&1; exit 1)
	@docker rm -f lb-dev-db > /dev/null
	@echo "tenant revision 생성 완료"

revision-all:
	$(MAKE) revision-public
	$(MAKE) revision-tenant

# public 마이그레이션 적용
migrate-public:
	$(LB) update --url="$(LB_URL)" --changelog-file=$(LB_CHANGELOG) --default-schema-name=public \
		--context-filter=public
	@echo "public 마이그레이션 완료"

# tenant 마이그레이션 적용 (모든 tenant_* 스키마 순회)
migrate-tenant:
	@SCHEMAS=$$(docker exec fabbit-db psql -U fabbit -t -A -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%'"); \
	if [ -z "$$SCHEMAS" ]; then \
		echo "적용할 tenant 스키마 없음 (skip)"; \
	else \
		for s in $$SCHEMAS; do \
			echo "tenant 마이그레이션 적용: $$s"; \
			$(LB) update --url="$(LB_URL)" --changelog-file=$(LB_CHANGELOG) \
				--default-schema-name=$$s --liquibase-schema-name=$$s \
				--context-filter=tenant; \
		done; \
		echo "tenant 마이그레이션 완료"; \
	fi

# public + tenant 마이그레이션 적용
migrate-all:
	$(MAKE) migrate-public
	$(MAKE) migrate-tenant

# ── 테스트 ──

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

# Java 포맷/임포트 및 아키텍처 규칙 검증 (DB 불필요)
lint:
#	./gradlew spotlessCheck archTest
	./gradlew archTest

format:
	./gradlew spotlessApply

# 3D 변환기 이미지 빌드 (linux/amd64)
build-3dconverter:
	docker build \
		--platform linux/amd64 \
		-f docker/Dockerfile.3dconverter \
		-t $(THREED_CONVERTER_IMAGE) \
		.
