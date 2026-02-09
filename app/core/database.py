"""SQLAlchemy 엔진, 세션, Base 설정.

AGE 확장 초기화를 connect 이벤트로 자동화하여
모든 새 물리 커넥션에 LOAD 'age' + search_path 설정을 보장합니다.
"""

from sqlalchemy import event, create_engine, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.core.config import settings

engine = create_engine(
    settings.database_url,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,
)


@event.listens_for(engine, "connect")
def _setup_age(dbapi_connection, connection_record):
    """물리 커넥션 생성 시 AGE 확장 초기화 (전역 설정만 담당)

    테넌트별 search_path 전환은 get_tenant_db()에서 요청마다 처리합니다.
    여기서는 ag_catalog만 등록하여 cypher() 함수 호출이 가능한 최소 환경을 구축합니다.
    """
    dbapi_connection.autocommit = True
    cursor = dbapi_connection.cursor()
    cursor.execute("LOAD 'age';")
    cursor.execute("SET search_path = ag_catalog, public;")
    cursor.close()
    dbapi_connection.autocommit = False


SessionLocal = sessionmaker(bind=engine)


class Base(DeclarativeBase):
    pass
