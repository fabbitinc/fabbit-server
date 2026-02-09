"""공통 Dependency.

인증, DB 세션 등 API 엔드포인트에서 공통으로 사용하는 의존성입니다.
현재는 인증 없이 org_id를 파라미터로 받습니다 (임시).
"""

from collections.abc import Generator

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core.database import SessionLocal


def get_db() -> Generator[Session, None, None]:
    """SQLAlchemy 세션 의존성 (요청 단위 생성/종료)

    테넌트 격리가 불필요한 엔드포인트에서 사용합니다.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_db(org_id: str) -> Generator[Session, None, None]:
    """테넌트 격리 세션 의존성 (요청마다 search_path 전환)

    커넥션 풀에서 꺼낸 커넥션에 이전 요청의 search_path가 남아있을 수 있으므로,
    매 요청마다 명시적으로 SET search_path를 실행하여 테넌트 격리를 보장합니다.

    사용법:
        @router.post("/some-endpoint")
        def some_endpoint(org_id: str, db: Session = Depends(get_tenant_db)):
            ...
    """
    db = SessionLocal()
    try:
        db.execute(text(f"SET search_path = tenant_{org_id}, ag_catalog, public"))
        yield db
    finally:
        db.close()
