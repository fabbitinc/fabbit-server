# Models 작성 규칙

## 선언 순서

1. `__tablename__`
2. `__table_args__`
3. 컬럼 (PK → FK → 일반 → timestamps)
4. relationships

## PK

- UUID v7 필수: `default=generate_uuid7` (`app.core.database`)
- `uuid_utils.UUID` 직접 사용 금지 (psycopg2 비호환)

## 인덱스 / 유니크

- `__table_args__`에서만 선언 — 컬럼 정의에 `unique=True`, `index=True` 금지
- FK 컬럼은 반드시 인덱스 (PostgreSQL은 FK 자동 인덱스 없음)
- 네이밍: `ix_{table}_{columns}`, `uq_{table}_{columns}`
- 목적을 한글 주석으로 명시

## Base 클래스

- `public` 스키마 → `Base` (Organization, User)
- `tenant_{org_id}` 스키마 → `TenantBase` (Project, ColumnMapping)

### TenantBase 규칙

- 프리픽스 없이 도메인 이름 사용 (`TenantProject` ✗ → `Project` ✓)
- `app/modules/{domain}/models.py`에 배치
- `provisioning.py`에서 import 필수 — `TenantBase.metadata.create_all()`이 인식하도록

## 컬럼 규칙

- **String 길이 필수**: `String(n)` — 길이 없는 `String` 사용 금지
- **Nullable**: `Mapped[str | None]` + `nullable=True` 페어로 일치시킬 것
- **FK ondelete 필수**: `CASCADE` 또는 `SET NULL` 명시
- **Timestamps**: `created_at` 필수 (`server_default=func.now()`), `updated_at`는 변경 가능 엔티티만 (`onupdate=func.now()` 추가)

## Relationship

- Cross-module은 `TYPE_CHECKING` import로 순환 참조 방지
- 문자열로 참조: `relationship("ModelName", ...)`
