"""테넌트 스키마 전용 Alembic 환경.

모든 tenant_* 스키마를 순회하며 TenantBase 모델 기반 마이그레이션을 적용합니다.
"""

import re
from logging.config import fileConfig

from sqlalchemy import create_engine, pool, text

from alembic import context

from app.core.config import settings
from app.core.database import TenantBase, discover_models

discover_models()

config = context.config

config.set_main_option("sqlalchemy.url", settings.database_url)

if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = TenantBase.metadata

# TenantBase에 정의된 테이블명 집합 — autogenerate 시 다른 스키마 테이블 무시용
_tenant_tables = set(target_metadata.tables.keys())


def _include_object(object, name, type_, reflected, compare_to):
    """search_path에 포함된 public/ag_catalog 테이블을 autogenerate에서 제외."""
    if type_ == "table":
        return name in _tenant_tables
    return True


_TENANT_SCHEMA_RE = re.compile(r"^tenant_[0-9a-f]{32}$")


def _get_tenant_schemas(connection) -> list[str]:
    """pg_namespace에서 tenant_* 스키마 목록 조회."""
    result = connection.execute(
        text("SELECT nspname FROM pg_namespace WHERE nspname LIKE 'tenant_%' ORDER BY nspname")
    )
    schemas = [row[0] for row in result]
    # SQL 구성에 사용되므로 스키마명 형식을 검증
    for schema in schemas:
        if not _TENANT_SCHEMA_RE.match(schema):
            raise ValueError(f"예상하지 못한 테넌트 스키마명: {schema}")
    return schemas


def run_migrations_offline() -> None:
    raise NotImplementedError("테넌트 마이그레이션은 offline 모드를 지원하지 않습니다.")


def run_migrations_online() -> None:
    connectable = create_engine(
        settings.database_url,
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        # AGE 초기화 (Alembic 전용 엔진이므로 connect 이벤트 없음)
        connection.execute(text("LOAD 'age'"))

        schemas = _get_tenant_schemas(connection)
        # autobegin 트랜잭션 정리 — 이후 begin_transaction()이 SAVEPOINT가 아닌 실제 트랜잭션을 시작하도록
        connection.commit()

        for schema in schemas:
            connection.execute(
                text(f"SET search_path = {schema}, ag_catalog, public")
            )
            # 스키마별로 alembic_version 테이블 격리
            context.configure(
                connection=connection,
                target_metadata=target_metadata,
                version_table_schema=schema,
                include_schemas=False,
                include_object=_include_object,
            )

            with context.begin_transaction():
                context.run_migrations()
            # SET search_path가 autobegin을 트리거하므로, begin_transaction()은 여전히 SAVEPOINT.
            # SAVEPOINT release 후 외부 트랜잭션을 명시 커밋해야 변경이 유지됨.
            connection.commit()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
