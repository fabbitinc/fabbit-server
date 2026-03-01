"""rename cr_state OPEN to SUBMITTED

Revision ID: d835d546b329
Revises: 534d55283874
Create Date: 2026-03-01 23:39:43.754036

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd835d546b329'
down_revision: Union[str, Sequence[str], None] = '534d55283874'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """cr_state enum: OPEN → SUBMITTED 변경.

    PostgreSQL은 ALTER TYPE ... RENAME VALUE를 지원하지 않으므로
    enum 타입을 재생성한다.
    """
    # 1) 기존 컬럼을 text로 임시 전환
    op.execute("ALTER TABLE change_requests ALTER COLUMN cr_state TYPE text")

    # 2) 기존 데이터 변환
    op.execute("UPDATE change_requests SET cr_state = 'SUBMITTED' WHERE cr_state = 'OPEN'")

    # 3) 기존 enum 타입 삭제 후 새로 생성
    op.execute("DROP TYPE cr_state")
    op.execute("CREATE TYPE cr_state AS ENUM ('DRAFT', 'SUBMITTED', 'MERGED', 'CLOSED')")

    # 4) 컬럼을 새 enum 타입으로 복원
    op.execute(
        "ALTER TABLE change_requests "
        "ALTER COLUMN cr_state TYPE cr_state USING cr_state::cr_state"
    )


def downgrade() -> None:
    """cr_state enum: SUBMITTED → OPEN 복원."""
    op.execute("ALTER TABLE change_requests ALTER COLUMN cr_state TYPE text")
    op.execute("UPDATE change_requests SET cr_state = 'OPEN' WHERE cr_state = 'SUBMITTED'")
    op.execute("DROP TYPE cr_state")
    op.execute("CREATE TYPE cr_state AS ENUM ('DRAFT', 'OPEN', 'MERGED', 'CLOSED')")
    op.execute(
        "ALTER TABLE change_requests "
        "ALTER COLUMN cr_state TYPE cr_state USING cr_state::cr_state"
    )
