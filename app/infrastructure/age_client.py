"""Apache AGE 커넥션 관리.

싱글턴 커넥션 (일반 API용)과 배치 전용 커넥션을 제공합니다.
Cypher 쿼리 실행 시 RETURN 컬럼 수를 자동 파싱하여 다중 컬럼을 지원합니다.
"""

import re

import psycopg2

from app.core.config import settings

GRAPH = settings.graph_name

_connection = None


def _setup_age(conn):
    """AGE 확장 초기화"""
    conn.autocommit = True
    with conn.cursor() as cur:
        cur.execute("LOAD 'age';")
        cur.execute("SET search_path = ag_catalog, '$user', public;")
    conn.autocommit = False


def get_connection():
    """싱글턴 커넥션 반환 (일반 API 요청용)"""
    global _connection
    if _connection is None or _connection.closed:
        _connection = psycopg2.connect(settings.database_dsn)
        _setup_age(_connection)
    return _connection


def create_connection():
    """새 커넥션 생성 (배치 인제스션용, 호출자가 직접 close 필요)"""
    conn = psycopg2.connect(settings.database_dsn)
    _setup_age(conn)
    return conn


def _count_return_columns(query: str) -> int:
    """RETURN 절의 컬럼 수를 파싱"""
    match = re.search(
        r'\bRETURN\b(.+?)(?:\bORDER\b|\bLIMIT\b|\bSKIP\b|$)',
        query,
        re.IGNORECASE | re.DOTALL,
    )
    if not match:
        return 1

    return_clause = match.group(1).strip()
    depth = 0
    count = 1
    for ch in return_clause:
        if ch in ('(', '[', '{'):
            depth += 1
        elif ch in (')', ']', '}'):
            depth -= 1
        elif ch == ',' and depth == 0:
            count += 1
    return count


def _build_cypher_sql(query: str) -> str:
    """Cypher 쿼리를 Apache AGE SQL로 래핑 (다중 컬럼 RETURN 지원)"""
    col_count = _count_return_columns(query)
    cols = ", ".join(f"c{i} agtype" for i in range(col_count))
    return f"SELECT * FROM cypher('{GRAPH}', $$ {query} $$) AS ({cols});"


def execute_cypher(query: str, conn=None) -> list:
    """Cypher 쿼리 실행 후 결과 반환 (다중 컬럼 지원)"""
    if conn is None:
        conn = get_connection()

    results = []
    try:
        with conn.cursor() as cur:
            sql = _build_cypher_sql(query)
            cur.execute(sql)
            col_names = [desc[0] for desc in cur.description]
            for row in cur:
                if len(col_names) == 1:
                    results.append(row[0])
                else:
                    results.append(dict(zip(col_names, row)))
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    return results


def execute_cypher_raw(query: str, conn=None):
    """Cypher 쿼리 실행 (결과 없는 MERGE/CREATE 등)"""
    if conn is None:
        conn = get_connection()

    try:
        with conn.cursor() as cur:
            sql = _build_cypher_sql(query)
            cur.execute(sql)
        conn.commit()
    except Exception:
        conn.rollback()
        raise


def execute_sql(query: str, params: tuple = None, conn=None) -> list:
    """일반 SQL 쿼리 실행 (column_mappings 테이블 등)"""
    if conn is None:
        conn = get_connection()

    results = []
    try:
        with conn.cursor() as cur:
            cur.execute(query, params)
            if cur.description:
                columns = [desc[0] for desc in cur.description]
                for row in cur:
                    results.append(dict(zip(columns, row)))
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    return results
