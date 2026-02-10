.PHONY: dev-start dev-stop dev-db-reset openapi

# 개발환경 시작 (PostgreSQL + API 서버)
dev-start:
	docker compose up -d
	@echo "PostgreSQL 준비 대기..."
	@until docker exec fabbit-db pg_isready -U fabbit -q 2>/dev/null; do sleep 0.5; done
	@echo "PostgreSQL 준비 완료"
	uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 개발환경 종료
dev-stop:
	-@lsof -ti:8000 | xargs kill 2>/dev/null || true
	docker compose down

# DB 초기화 (볼륨 삭제)
dev-db-reset:
	docker compose down -v
	@echo "DB 볼륨 삭제 완료. make dev-start로 재시작하세요."

# OpenAPI 스펙 파일 생성
openapi:
	@curl -s http://localhost:8000/openapi.json | python3 -m json.tool > openapi.json
	@echo "openapi.json 생성 완료"
	@cp ./openapi.json ../web/openapi.json
	@echo "openapi.json 복사 완료"

dev-reset:
	$(MAKE) dev-stop
	$(MAKE) dev-db-reset
	$(MAKE) dev-start