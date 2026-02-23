# ── Stage 1: 의존성 설치 ──
FROM python:3.12-slim AS deps

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

# ── Stage 2: 런타임 ──
FROM python:3.12-slim AS runtime

# 비root 사용자
RUN groupadd -g 1001 app && useradd -u 1001 -g app -s /bin/false app

WORKDIR /app

# 가상환경 + 소스 복사
COPY --from=deps /app/.venv .venv
COPY app/ app/
COPY alembic/ alembic/
COPY alembic.ini .

ENV PATH="/app/.venv/bin:$PATH"

USER 1001
EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
