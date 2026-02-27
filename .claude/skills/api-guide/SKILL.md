---
name: api-guide
description: "API 레이어 작성 규칙. router, 엔드포인트, Depends, docstring 코드를 작성하거나 수정할 때 자동 참조. 라우터 구조, 의존성 주입, OpenAPI 문서 규칙을 제공."
user-invocable: false
---

# API Layer 작성 규칙

## 역할

- HTTP 요청/응답 처리만 담당 — 비즈니스 로직 금지
- use_case/queries 호출 후 결과 반환하는 얇은 레이어

## 라우터 구조

- `api/v1/public/` — 인증 불필요 (auth, health 등)
- `api/v1/tenant/` — 인증 필수 + 테넌트 격리
- 파일명: `{domain}_router.py`
- prefix: `/api/v1/{domain}`

### Sub-router 그룹핑

sub-router가 여러 개인 도메인은 디렉토리로 묶고 `__init__.py`에서 단일 `router`로 합친다.

```
api/v1/tenant/project/
├── __init__.py              # router = APIRouter() + include_router(각 sub-router)
├── project_router.py        # /api/v1/projects
├── project_issue_router.py  # /api/v1/projects/{project_id}/issues
└── project_label_router.py  # /api/v1/projects/{project_id}/labels
```

- `__init__.py`의 `APIRouter()`는 prefix 없음 — 각 sub-router가 자체 prefix 보유
- `main.py`는 `from app.api.v1.tenant.project import router`로 단일 import

## 의존성 주입 (Depends)

- `require_auth` → `AuthContext` (인증 필수)
- `get_tenant_db` → `Session` (테넌트 격리 세션, 인증 포함)
- `get_db` → `Session` (public 스키마, 인증 불필요)

## Docstring (OpenAPI 문서)

- 모든 엔드포인트 함수에 docstring 필수 — Swagger UI 설명으로 자동 반영
- 첫 줄: 한 문장 요약 (무엇을 하는지)
- 본문: 동작 방식, 주요 파라미터 설명, 상태 흐름 등 프론트엔드 개발자가 참고할 내용
- 마크다운 지원 — `**bold**`, 리스트(`-`), 코드(`backtick`) 활용 가능

## Import alias 컨벤션

- `from app.use_cases import {domain} as {domain}_commands`
- `from app.queries import {domain} as {domain}_queries`

## 규칙

- 읽기: queries를 직접 호출
- 쓰기: use_case를 호출 (service 직접 호출 금지)
- 요청/응답은 Pydantic schema로 타입 지정 (`response_model=`)
- 로깅 최소화 — HTTP 요청/응답은 OTel이 처리
