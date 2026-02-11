"""rename plan_type FREE to STARTER

Revision ID: 56fdf39cf789
Revises: e142bc82b123
Create Date: 2026-02-12 01:08:36.861466

"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = '56fdf39cf789'
down_revision: Union[str, Sequence[str], None] = 'e142bc82b123'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """기존 플랜 타입 이름 변경: FREE→STARTER, PRO→TEAM, ELITE→ENTERPRISE."""
    op.execute("UPDATE organizations SET plan_type = 'STARTER' WHERE plan_type = 'FREE'")
    op.execute("UPDATE organizations SET plan_type = 'TEAM' WHERE plan_type = 'PRO'")
    op.execute("UPDATE organizations SET plan_type = 'ENTERPRISE' WHERE plan_type = 'ELITE'")
    op.alter_column('organizations', 'plan_type', server_default='STARTER')


def downgrade() -> None:
    """Downgrade schema."""
    op.execute("UPDATE organizations SET plan_type = 'FREE' WHERE plan_type = 'STARTER'")
    op.execute("UPDATE organizations SET plan_type = 'PRO' WHERE plan_type = 'TEAM'")
    op.execute("UPDATE organizations SET plan_type = 'ELITE' WHERE plan_type = 'ENTERPRISE'")
    op.alter_column('organizations', 'plan_type', server_default='FREE')
