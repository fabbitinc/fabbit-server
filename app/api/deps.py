"""공통 Dependency.

인증, DB 세션 등 API 엔드포인트에서 공통으로 사용하는 의존성입니다.
현재는 인증 없이 org_id를 파라미터로 받습니다 (임시).
"""

from collections.abc import Generator

from sqlalchemy.orm import Session

from app.core.database import SessionLocal


def get_db() -> Generator[Session, None, None]:
    """SQLAlchemy 세션 의존성 (요청 단위 생성/종료)"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
