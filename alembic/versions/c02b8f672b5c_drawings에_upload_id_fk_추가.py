"""drawings에 upload_id FK 추가

Revision ID: c02b8f672b5c
Revises: 39d26151ca19
Create Date: 2026-02-21 07:59:36.872491

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c02b8f672b5c'
down_revision: Union[str, Sequence[str], None] = '39d26151ca19'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def _get_tenant_schemas() -> list[str]:
    """현재 DB의 모든 tenant_* 스키마 조회."""
    conn = op.get_bind()
    rows = conn.execute(
        sa.text(
            "SELECT schema_name FROM information_schema.schemata "
            "WHERE schema_name LIKE 'tenant_%'"
        )
    ).fetchall()
    return [r[0] for r in rows]


def upgrade() -> None:
    """각 테넌트 스키마의 drawings 테이블에 upload_id 컬럼 + FK + 인덱스 추가."""
    for schema in _get_tenant_schemas():
        op.add_column(
            "drawings",
            sa.Column("upload_id", sa.UUID(), nullable=True),
            schema=schema,
        )
        op.create_foreign_key(
            f"fk_drawings_upload_id_{schema}",
            "drawings",
            "uploads",
            ["upload_id"],
            ["id"],
            ondelete="SET NULL",
            source_schema=schema,
            referent_schema=schema,
        )
        op.create_index(
            "ix_drawings_upload_id",
            "drawings",
            ["upload_id"],
            schema=schema,
        )


def downgrade() -> None:
    """각 테넌트 스키마에서 upload_id 관련 객체 제거."""
    for schema in _get_tenant_schemas():
        op.drop_index(
            "ix_drawings_upload_id",
            table_name="drawings",
            schema=schema,
        )
        op.drop_constraint(
            f"fk_drawings_upload_id_{schema}",
            "drawings",
            schema=schema,
            type_="foreignkey",
        )
        op.drop_column("drawings", "upload_id", schema=schema)
