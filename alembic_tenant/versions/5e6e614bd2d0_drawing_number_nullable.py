"""drawing_number nullable

Revision ID: 5e6e614bd2d0
Revises: a0970e9a6683
Create Date: 2026-02-24 14:58:23.380143

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '5e6e614bd2d0'
down_revision: Union[str, Sequence[str], None] = 'a0970e9a6683'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.alter_column("drawings", "drawing_number", existing_type=sa.String(100), nullable=True)


def downgrade() -> None:
    """Downgrade schema."""
    op.alter_column("drawings", "drawing_number", existing_type=sa.String(100), nullable=False)
