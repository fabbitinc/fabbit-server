"""mapping_records에 sheet_name 컬럼 추가

Revision ID: 98b923fcbb07
Revises: 0d7f39f3f3c7
Create Date: 2026-02-10 20:32:34.041015

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy import text


# revision identifiers, used by Alembic.
revision: str = '98b923fcbb07'
down_revision: Union[str, Sequence[str], None] = '0d7f39f3f3c7'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def _get_tenant_schemas() -> list[str]:
    """모든 tenant_* 스키마 이름을 조회."""
    conn = op.get_bind()
    result = conn.execute(
        text("SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%'")
    )
    return [row[0] for row in result]


def upgrade() -> None:
    """모든 테넌트 스키마의 mapping_records에 sheet_name 컬럼 추가."""
    for schema in _get_tenant_schemas():
        op.add_column(
            "mapping_records",
            sa.Column("sheet_name", sa.String(200), nullable=True),
            schema=schema,
        )


def downgrade() -> None:
    """모든 테넌트 스키마의 mapping_records에서 sheet_name 컬럼 제거."""
    for schema in _get_tenant_schemas():
        op.drop_column("mapping_records", "sheet_name", schema=schema)
