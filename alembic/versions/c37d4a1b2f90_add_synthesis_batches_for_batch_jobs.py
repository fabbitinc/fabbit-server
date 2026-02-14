"""add synthesis batches for batch jobs

Revision ID: c37d4a1b2f90
Revises: 56fdf39cf789
Create Date: 2026-02-14 05:40:00.000000

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy import text
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "c37d4a1b2f90"
down_revision: Union[str, Sequence[str], None] = "56fdf39cf789"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def _get_tenant_schemas() -> list[str]:
    """모든 tenant_* 스키마 이름을 조회."""
    conn = op.get_bind()
    result = conn.execute(
        text(
            "SELECT schema_name FROM information_schema.schemata "
            "WHERE schema_name LIKE 'tenant_%'"
        )
    )
    return [row[0] for row in result]


def upgrade() -> None:
    """모든 테넌트 스키마에 합성 배치 테이블/컬럼을 추가."""
    for schema in _get_tenant_schemas():
        op.create_table(
            "synthesis_batches",
            sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
            sa.Column(
                "project_id",
                postgresql.UUID(as_uuid=True),
                nullable=False,
            ),
            sa.Column(
                "mapping_id",
                postgresql.UUID(as_uuid=True),
                nullable=False,
            ),
            sa.Column("requested_count", sa.Integer(), nullable=False),
            sa.Column("accepted_count", sa.Integer(), nullable=False),
            sa.Column(
                "failed_uploads",
                postgresql.JSONB(astext_type=sa.Text()),
                nullable=False,
            ),
            sa.Column(
                "created_at",
                sa.DateTime(timezone=True),
                server_default=sa.text("now()"),
                nullable=False,
            ),
            sa.ForeignKeyConstraint(
                ["project_id"],
                [f"{schema}.projects.id"],
                ondelete="CASCADE",
            ),
            sa.ForeignKeyConstraint(
                ["mapping_id"],
                [f"{schema}.mapping_records.id"],
                ondelete="CASCADE",
            ),
            sa.PrimaryKeyConstraint("id"),
            schema=schema,
        )
        op.create_index(
            "ix_synthesis_batches_project_id",
            "synthesis_batches",
            ["project_id"],
            unique=False,
            schema=schema,
        )

        op.add_column(
            "synthesis_jobs",
            sa.Column("batch_id", postgresql.UUID(as_uuid=True), nullable=True),
            schema=schema,
        )
        op.create_index(
            "ix_synthesis_jobs_batch_id",
            "synthesis_jobs",
            ["batch_id"],
            unique=False,
            schema=schema,
        )
        op.create_foreign_key(
            "fk_synthesis_jobs_batch_id",
            "synthesis_jobs",
            "synthesis_batches",
            ["batch_id"],
            ["id"],
            source_schema=schema,
            referent_schema=schema,
            ondelete="SET NULL",
        )


def downgrade() -> None:
    """모든 테넌트 스키마에서 합성 배치 테이블/컬럼을 제거."""
    for schema in _get_tenant_schemas():
        op.drop_index(
            "ix_synthesis_batches_project_id",
            table_name="synthesis_batches",
            schema=schema,
        )
        op.drop_constraint(
            "fk_synthesis_jobs_batch_id",
            "synthesis_jobs",
            schema=schema,
            type_="foreignkey",
        )
        op.drop_index(
            "ix_synthesis_jobs_batch_id", table_name="synthesis_jobs", schema=schema
        )
        op.drop_column("synthesis_jobs", "batch_id", schema=schema)
        op.drop_table("synthesis_batches", schema=schema)
