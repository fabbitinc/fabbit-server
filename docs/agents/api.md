# API Layer 작성 규칙

## 역할

- HTTP 요청/응답 처리만 담당 — 비즈니스 로직 금지
- service 호출 후 결과 반환하는 얇은 레이어

## 라우터 구조

- `api/v1/public/` — 인증 불필요 (auth, health 등)
- `api/v1/tenant/` — 인증 필수 + 테넌트 격리
- 파일명: `{domain}_router.py`
- prefix: `/api/v1/{domain}`

## 의존성 주입 (Depends)

- `require_auth` → `AuthContext` (인증 필수)
- `get_tenant_db` → `Session` (테넌트 격리 세션, 인증 포함)
- `get_db` → `Session` (public 스키마, 인증 불필요)

## 규칙

- service를 모듈로 import: `from app.modules.{domain} import service`
- 요청/응답은 Pydantic schema로 타입 지정 (`response_model=`)
- 로깅 최소화 — HTTP 요청/응답은 OTel이 처리
