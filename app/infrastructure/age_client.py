"""Apache AGE Cypher 실행 유틸리티.

모든 실행 함수가 SQLAlchemy Session을 인자로 받습니다.
AGE 초기화(LOAD 'age')는 database.py의 connect 이벤트로 자동 처리됩니다.

exec_driver_sql() + $$ 달러 쿼팅으로 AGE Cypher를 실행합니다.
- AGE는 Cypher 인자로 dollar-quoted string을 요구 (%s 바인딩은 '...'로 전달되어 거부됨)
- exec_driver_sql은 text()와 달리 :LabelName을 바인드 파라미터로 파싱하지 않아 콜론 충돌 없음
- Session 트랜잭션을 그대로 유지하므로 db.commit()/db.rollback()이 정상 동작
"""

import json
import re
from typing import Any

from sqlalchemy.orm import Session

from app.core.config import settings


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


def _parse_agtype(val: Any) -> Any:
    """AGE의 agtype 값을 Python 객체로 변환

    '{"id": 123, ...}::vertex' → dict
    '"Steel"' → "Steel"
    """
    if not isinstance(val, str):
        return val
    clean = re.sub(r'::\w+$', '', val)
    try:
        return json.loads(clean)
    except (json.JSONDecodeError, ValueError):
        return val


def execute_cypher(
    db: Session,
    query: str,
    graph_name: str = settings.graph_name,
) -> list:
    """Cypher 쿼리 실행 후 결과 반환 (다중 컬럼 지원)

    exec_driver_sql + $$ 달러 쿼팅으로 실행하여 Session 트랜잭션을 유지합니다.
    """
    query = query.strip().rstrip(";")
    col_count = _count_return_columns(query)
    cols = ", ".join(f"c{i} agtype" for i in range(col_count))
    wrapped_sql = f"SELECT * FROM cypher('{graph_name}', $${query}$$) AS ({cols});"

    conn = db.connection()
    result = conn.exec_driver_sql(wrapped_sql)

    rows = []
    for row in result:
        parsed = [_parse_agtype(col) for col in row]
        if col_count == 1:
            rows.append(parsed[0])
        else:
            col_names = [f"c{i}" for i in range(col_count)]
            rows.append(dict(zip(col_names, parsed)))
    return rows


def execute_cypher_raw(
    db: Session,
    query: str,
    graph_name: str = settings.graph_name,
) -> None:
    """Cypher 쿼리 실행 (결과 없는 MERGE/CREATE 등, 커밋은 호출자가 관리)"""
    query = query.strip().rstrip(";")
    wrapped_sql = f"SELECT * FROM cypher('{graph_name}', $${query}$$) AS (v agtype);"
    db.connection().exec_driver_sql(wrapped_sql)
