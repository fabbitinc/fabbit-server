"""SQLAlchemy 엔진, 세션, Base 설정.

AGE 확장 초기화를 connect 이벤트로 자동화하여
모든 새 물리 커넥션에 LOAD 'age' + search_path 설정을 보장합니다.
"""

import uuid as _uuid

from sqlalchemy import event, create_engine, text
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker
from uuid_utils import uuid7 as _uuid7

from app.core.config import settings


def generate_uuid7() -> _uuid.UUID:
    """UUID v7 생성 (시간순 정렬 PK용, psycopg2 호환 표준 uuid.UUID 반환)."""
    return _uuid.UUID(bytes=_uuid7().bytes)

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


def create_tenant_session(schema_name: str) -> Session:
    """배경 태스크용 테넌트 격리 세션.

    after_begin 이벤트로 매 트랜잭션 시작 시 search_path를 재설정하여
    commit() 후 커넥션 교체 시에도 테넌트 격리를 보장합니다.
    """
    db = SessionLocal()

    @event.listens_for(db, "after_begin")
    def _restore_search_path(session, transaction, connection):
        connection.execute(text(f"SET search_path = {schema_name}, ag_catalog, public"))

    return db


class Base(DeclarativeBase):
    """public 스키마 ORM 모델 베이스."""
    pass


class TenantBase(DeclarativeBase):
    """테넌트 스키마 ORM 모델 베이스.

    search_path가 전환된 상태에서 metadata.create_all()로 테이블 생성.
    public.Base와 분리하여 create_all() 시 테넌트 테이블만 대상으로 함.
    """
    pass
