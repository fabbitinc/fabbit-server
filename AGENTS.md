# Fabbit Server

@docs/agents/project.md

## 레이어별 규칙

### core

### api

@docs/agents/api.md

### infrastructure

@docs/agents/infrastructure.md
@docs/agents/database.md

### modules

- `schemas.py` — Pydantic 요청/응답 모델. 새 API 추가 시 반드시 정의
- `constants.py` — 도메인 상수/Enum. 매직 넘버 대신 여기서 관리

@docs/agents/models.md
@docs/agents/service.md
@docs/agents/repository.md

## 로깅 규칙

@docs/agents/logging.md
