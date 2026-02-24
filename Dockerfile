# ── Stage 1: QCAD 추출 ──
FROM ubuntu:22.04 AS qcad-extract
ADD https://www.qcad.org/archives/qcad/qcad-3.32.6-trial-linux-x86_64.tar.gz /tmp/qcad.tar.gz
RUN mkdir -p /opt/qcad && tar xzf /tmp/qcad.tar.gz -C /opt/qcad --strip-components=1

# ── Stage 2: 의존성 설치 ──
FROM python:3.12-slim AS deps

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

# ── Stage 3: 런타임 ──
FROM python:3.12-slim AS runtime

# QCAD headless 런타임 라이브러리
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 libgl1 libxrender1 libfontconfig1 libxkbcommon0 \
    libx11-6 libxext6 libxi6 libxcb1 libdbus-1-3 \
    fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

# 비root 사용자
RUN groupadd -g 1001 app && useradd -u 1001 -g app -s /bin/false app

# QCAD 바이너리
COPY --from=qcad-extract /opt/qcad /opt/qcad
ENV PATH="/opt/qcad:${PATH}"

# 임시 파일 디렉토리
RUN mkdir -p /tmp/drawing-converter && chown 1001:1001 /tmp/drawing-converter

WORKDIR /app

# 가상환경 + 소스 복사
COPY --from=deps /app/.venv .venv
COPY app/ app/
COPY alembic/ alembic/
COPY alembic_tenant/ alembic_tenant/
COPY alembic.ini .

ENV PATH="/app/.venv/bin:$PATH"

USER 1001
EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
