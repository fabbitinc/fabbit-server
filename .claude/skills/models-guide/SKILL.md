---
name: models-guide
description: "SQLAlchemy 모델 작성 규칙. models.py, 컬럼 정의, 인덱스, relationship, 도메인 메서드, 팩토리 메서드, 상태 Enum을 작성하거나 수정할 때 자동 참조."
user-invocable: false
---

# Models 작성 규칙

## 선언 순서

1. `__tablename__`
2. `__table_args__`
3. 컬럼 (PK → FK → 일반 → timestamps)
4. relationships
5. 도메인 메서드

## PK

- `PkMixin`을 사용 — PK 컬럼을 모델에 직접 선언하지 않는다
- `uuid_utils.UUID` 직접 사용 금지 (psycopg2 비호환)

## 인덱스 / 유니크

- `__table_args__`에서만 선언 — 컬럼 정의에 `unique=True`, `index=True` 금지
- FK 컬럼은 반드시 인덱스 (PostgreSQL은 FK 자동 인덱스 없음)
- 네이밍: `ix_{table}_{columns}`, `uq_{table}_{columns}`
- 목적을 한글 주석으로 명시

## Base 클래스

모델이 속하는 스키마에 따라 Base를 선택한다. 위치: `app/core/database.py`

| Base | 스키마 | 대상 | 마이그레이션 |
|------|--------|------|-------------|
| `Base` | `public` | 전역 엔티티 (User, Organization, Membership) | `alembic/` (public) |
| `TenantBase` | `tenant_{org_id}` | 테넌트 격리 엔티티 (Project, Part, Supplier) | `alembic_tenant/` |

### 선택 기준

- 조직과 무관하게 **전역으로 공유**되는 데이터 → `Base`
- 조직별로 **격리**되어야 하는 비즈니스 데이터 → `TenantBase`

### TenantBase 규칙

- 프리픽스 없이 도메인 이름 사용 (`TenantProject` ✗ → `Project` ✓)
- `app/modules/{domain}/models.py`에 배치
- 수동 import 불필요 — `discover_models()`가 `app/modules/*/models.py`를 자동 탐색

## Mixin

공통 컬럼은 mixin으로 선언한다. PK·timestamps를 모델에 직접 선언하지 않는다. 위치: `app/core/mixins.py`

| Mixin | 제공 컬럼 | 용도 |
|-------|-----------|------|
| `PkMixin` | `id` (UUID v7) | 모든 모델 |
| `TimestampMixin` | `created_at` | append-only, immutable 엔티티 |
| `UpdatableMixin` | `created_at` + `updated_at` | 변경 가능 엔티티 (TimestampMixin 상속) |
| `AuditMixin` | `created_by` + `updated_by` | 감사 이력이 필요한 엔티티. UUID 논리적 참조 (cross-schema FK 없음) |
| `SoftDeleteMixin` | `deleted_at` + `soft_delete()` + `is_deleted` | 소프트 삭제가 필요한 엔티티 |

### 조합 규칙

- **모든 모델**: `PkMixin` 필수
- **timestamps 선택**: `TimestampMixin` 또는 `UpdatableMixin` 중 하나 (변경 가능 여부로 판단)
- **감사 추적**: 누가 생성/수정했는지 기록이 필요하면 `AuditMixin` 추가 조합
- **소프트 삭제**: 필요 시 `SoftDeleteMixin` 추가 조합

### 상속 순서 (MRO)

**Base/TenantBase는 반드시 `__bases__`의 마지막에 위치해야 한다.** `linter/check_model_mro.py`가 자동 검증한다.

```python
# 올바름 — Mixin → Base 순서, Base가 마지막
class Part(UpdatableMixin, PkMixin, TenantBase): ...

# 위반 — TenantBase 뒤에 Mixin
class Part(TenantBase, PkMixin): ...
```

### 예시

```python
from app.core.database import TenantBase
from app.core.mixins import PkMixin, UpdatableMixin

class Project(UpdatableMixin, PkMixin, TenantBase):
    __tablename__ = "projects"
    name: Mapped[str] = mapped_column(String(200), nullable=False)
```

## 컬럼 규칙

- **String 길이 필수**: `String(n)` — 길이 없는 `String` 사용 금지
- **Nullable**: `Mapped[str | None]` + `nullable=True` 페어로 일치시킬 것
- **FK ondelete 필수**: `CASCADE` 또는 `SET NULL` 명시
- **PK·Timestamps**: mixin으로 선언 — 모델에 `id`, `created_at`, `updated_at`을 직접 정의하지 않는다

## Relationship

- Cross-module은 `TYPE_CHECKING` import로 순환 참조 방지
- 문자열로 참조: `relationship("ModelName", ...)`

## 도메인 메서드 (Rich Domain Model)

모델은 단순 데이터 컨테이너가 아니라, **자기 필드를 변경하는 로직의 소유자**다.

### 상태 전이 메서드

상태 필드가 있는 모델은 **단순 토글이라도** 반드시 메서드로 제공한다.

- 상태값 + 부수 필드를 한 메서드에서 원자적으로 변경
- 메서드명은 동작 의도를 표현: `mark_*`, `complete_*`, `fail_*`, `request_*`, `activate`/`deactivate`
- 단순 on/off도 메서드화 — 나중에 부수 효과 추가 시 변경 지점이 하나

### 팩토리 메서드 (`@classmethod`)

생성 시 검증·초기화 로직이 있는 모델은 `@classmethod` 팩토리를 제공한다.

- `__init__` 오버라이드 금지 — SQLAlchemy ORM이 내부적으로 사용
- 팩토리에서 필드 검증, 기본값 조합, 파생 필드 계산 등을 캡슐화
- 호출부(service)는 팩토리를 통해 인스턴스 생성

### 적용 기준

| 패턴 | 적용 조건 |
|------|-----------|
| 상태 전이 메서드 | 상태 필드가 있는 모델 전부 (토글 포함) |
| 팩토리 메서드 | 생성 시 검증·초기화가 필요한 모델 |
| 해당 없음 | immutable 이력, append-only 로그 등 순수 기록 엔티티 |

### 공통 규칙

- 모델 메서드는 **자기 필드만 변경** — 다른 모델·외부 서비스 호출 금지
- 여러 도메인을 걸치는 오케스트레이션은 service에 유지

참고 구현: `app/modules/file/models.py`

## 상태 Enum

상태성 필드는 `constants.py`에 `str, Enum`으로 정의한다.

- 위치: `app/modules/{domain}/constants.py`
- 패턴: `class FileStatus(str, Enum)` — `str` 믹스인으로 JSON 직렬화 호환
- 각 값에 한글 주석으로 의미 명시
- 모델 컬럼의 `default`에 Enum 멤버 사용: `default=FileStatus.PENDING`
- 매직 스트링 리터럴(`default="PENDING"`) 사용 금지

참고 구현: `app/modules/file/constants.py`
