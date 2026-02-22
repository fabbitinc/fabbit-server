"""file_id를 original_file_id로 리네임, Drawing에 pdf_file_id/thumbnail_file_id 추가, File에서 변환 필드 제거

Revision ID: 506d511eec2c
Revises: 39d26151ca19
Create Date: 2026-02-22 13:08:59.613366

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID


# revision identifiers, used by Alembic.
revision: str = '506d511eec2c'
down_revision: Union[str, Sequence[str], None] = '39d26151ca19'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def _get_tenant_schemas() -> list[str]:
    """현재 DB에 존재하는 모든 tenant_* 스키마 목록 반환."""
    conn = op.get_bind()
    result = conn.execute(
        sa.text(
            "SELECT schema_name FROM information_schema.schemata "
            "WHERE schema_name LIKE 'tenant_%'"
        )
    )
    return [row[0] for row in result]


def upgrade() -> None:
    """Upgrade schema."""
    for schema in _get_tenant_schemas():
        # ── drawings 테이블 ──
        # file_id → original_file_id 리네임
        op.alter_column(
            "drawings", "file_id",
            new_column_name="original_file_id",
            schema=schema,
        )
        # 기존 인덱스 제거 후 새 이름으로 생성
        op.drop_index("ix_drawings_file_id", table_name="drawings", schema=schema)
        op.create_index(
            "ix_drawings_original_file_id", "drawings",
            ["original_file_id"], schema=schema,
        )

        # pdf_file_id 컬럼 + FK + 인덱스 추가
        op.add_column(
            "drawings",
            sa.Column("pdf_file_id", UUID(as_uuid=True), nullable=True),
            schema=schema,
        )
        op.create_foreign_key(
            "fk_drawings_pdf_file_id", "drawings", "files",
            ["pdf_file_id"], ["id"],
            source_schema=schema, referent_schema=schema,
            ondelete="SET NULL",
        )
        op.create_index(
            "ix_drawings_pdf_file_id", "drawings",
            ["pdf_file_id"], schema=schema,
        )

        # thumbnail_file_id 컬럼 + FK + 인덱스 추가
        op.add_column(
            "drawings",
            sa.Column("thumbnail_file_id", UUID(as_uuid=True), nullable=True),
            schema=schema,
        )
        op.create_foreign_key(
            "fk_drawings_thumbnail_file_id", "drawings", "files",
            ["thumbnail_file_id"], ["id"],
            source_schema=schema, referent_schema=schema,
            ondelete="SET NULL",
        )
        op.create_index(
            "ix_drawings_thumbnail_file_id", "drawings",
            ["thumbnail_file_id"], schema=schema,
        )

        # ── files 테이블 — 변환 관련 컬럼 제거 ──
        op.drop_column("files", "conversion_status", schema=schema)
        op.drop_column("files", "pdf_key", schema=schema)
        op.drop_column("files", "thumbnail_key", schema=schema)


def downgrade() -> None:
    """Downgrade schema."""
    for schema in _get_tenant_schemas():
        # ── files 테이블 — 변환 관련 컬럼 복원 ──
        op.add_column(
            "files",
            sa.Column("conversion_status", sa.String(20), nullable=True),
            schema=schema,
        )
        op.add_column(
            "files",
            sa.Column("pdf_key", sa.String(500), nullable=True),
            schema=schema,
        )
        op.add_column(
            "files",
            sa.Column("thumbnail_key", sa.String(500), nullable=True),
            schema=schema,
        )

        # ── drawings 테이블 ──
        # thumbnail_file_id 제거
        op.drop_index("ix_drawings_thumbnail_file_id", table_name="drawings", schema=schema)
        op.drop_constraint("fk_drawings_thumbnail_file_id", "drawings", schema=schema)
        op.drop_column("drawings", "thumbnail_file_id", schema=schema)

        # pdf_file_id 제거
        op.drop_index("ix_drawings_pdf_file_id", table_name="drawings", schema=schema)
        op.drop_constraint("fk_drawings_pdf_file_id", "drawings", schema=schema)
        op.drop_column("drawings", "pdf_file_id", schema=schema)

        # original_file_id → file_id 복원
        op.drop_index("ix_drawings_original_file_id", table_name="drawings", schema=schema)
        op.alter_column(
            "drawings", "original_file_id",
            new_column_name="file_id",
            schema=schema,
        )
        op.create_index(
            "ix_drawings_file_id", "drawings",
            ["file_id"], schema=schema,
        )
