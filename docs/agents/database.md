# 데이터베이스 — 멀티테넌트

## Schema-per-Tenant

| 스키마 | 용도 | 데이터 |
|--------|------|--------|
| `public` | 전역 공유 | organizations, users, subscriptions |
| `tenant_{org_id}` | 테넌트 격리 | projects, column_mappings, AGE 그래프 |

## 테넌트 세션

- `get_tenant_db` (`app/api/deps.py`) — 인증 기반 search_path 전환
- `after_begin` 이벤트로 매 트랜잭션마다 search_path 재설정 (commit 후 커넥션 반환 대비)
- `get_db` — public 스키마 전용 (테넌트 격리 불필요 시)

## 마이그레이션 (Alembic)

- **public 트랙**: `alembic upgrade head` → `Base` 모델
- **tenant 트랙**: 모든 `tenant_*` 스키마 순회 → `TenantBase` 모델 변경 적용

## 테넌트 프로비저닝

- `app/modules/auth/provisioning.py`에서 단일 트랜잭션 실행
- 순서: AGE 그래프 생성 → 테넌트 테이블 생성 → vlabel + B-tree 인덱스
