# Models 작성 규칙

## 선언 순서

1. `__tablename__`
2. `__table_args__`
3. 컬럼 (PK → FK → 일반 → timestamps)
4. relationships
5. 도메인 메서드

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
- `tenant_{org_id}` 스키마 → `TenantBase` (Project, MappingRecord)

### TenantBase 규칙

- 프리픽스 없이 도메인 이름 사용 (`TenantProject` ✗ → `Project` ✓)
- `app/modules/{domain}/models.py`에 배치
- 수동 import 불필요 — `discover_models()`가 `app/modules/*/models.py`를 자동 탐색

## 컬럼 규칙

- **String 길이 필수**: `String(n)` — 길이 없는 `String` 사용 금지
- **Nullable**: `Mapped[str | None]` + `nullable=True` 페어로 일치시킬 것
- **FK ondelete 필수**: `CASCADE` 또는 `SET NULL` 명시
- **Timestamps**: `created_at` 필수 (`server_default=func.now()`), `updated_at`는 변경 가능 엔티티만 (`onupdate=func.now()` 추가)

## Relationship

- Cross-module은 `TYPE_CHECKING` import로 순환 참조 방지
- 문자열로 참조: `relationship("ModelName", ...)`

## 도메인 메서드 (Rich Domain Model)

모델은 단순 데이터 컨테이너가 아니라, **자기 필드를 변경하는 로직의 소유자**다.
service/repository에서 모델 필드를 직접 조작(`file.status = ...`)하지 않고, 의도가 드러나는 모델 메서드를 호출한다.

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

> Python은 `private` 필드를 강제할 수 없으므로 직접 생성(`Model(field=...)`)을 언어 수준으로 막을 수 없다. 규칙 기반으로 팩토리 사용을 준수한다.

### 적용 기준

| 패턴 | 적용 조건 |
|------|-----------|
| 상태 전이 메서드 | 상태 필드가 있는 모델 전부 (토글 포함) |
| 팩토리 메서드 | 생성 시 검증·초기화가 필요한 모델 |
| 해당 없음 | immutable 이력(PartRevision), append-only 로그(AiUsageLog) 등 순수 기록 엔티티 |

### 공통 규칙

- 모델 메서드는 **자기 필드만 변경** — 다른 모델·외부 서비스 호출 금지
- 여러 도메인을 걸치는 오케스트레이션은 service에 유지

참고 구현: `app/modules/file/models.py`

## 상태 Enum

상태성 필드(`status`, `conversion_status` 등)는 `constants.py`에 `str, Enum`으로 정의한다.

- 위치: `app/modules/{domain}/constants.py`
- 패턴: `class FileStatus(str, Enum)` — `str` 믹스인으로 JSON 직렬화 호환
- 각 값에 한글 주석으로 의미 명시
- 모델 컬럼의 `default`에 Enum 멤버 사용: `default=FileStatus.PENDING`
- 매직 스트링 리터럴(`default="PENDING"`) 사용 금지

참고 구현: `app/modules/file/constants.py`
