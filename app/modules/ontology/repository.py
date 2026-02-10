"""온톨로지 Cypher 값 포맷팅 유틸리티.

Cypher 쿼리에 삽입할 값의 이스케이프 및 타입별 포맷팅을 담당합니다.
"""

import re

import pandas as pd


def escape_cypher_value(value) -> str:
    """Cypher 문자열 값 이스케이프 (injection 방지)"""
    if pd.isna(value) or value is None:
        return ""
    s = str(value).strip()
    s = s.replace("\\", "\\\\")
    s = s.replace("'", "\\'")
    s = re.sub(r"[;\-]{2,}", "", s)
    return s


def format_cypher_value(value, data_type: str = "string") -> str | None:
    """데이터 타입에 맞게 Cypher 리터럴 포맷팅

    - string  → '값' (따옴표)
    - integer → 123 (따옴표 없음)
    - float   → 12.5 (따옴표 없음)
    - boolean → true/false (따옴표 없음)
    """
    if pd.isna(value) or value is None or str(value).strip() == "":
        return None

    if data_type == "integer":
        try:
            return str(int(float(value)))
        except (ValueError, TypeError):
            return f"'{escape_cypher_value(value)}'"

    if data_type == "float":
        try:
            return str(float(value))
        except (ValueError, TypeError):
            return f"'{escape_cypher_value(value)}'"

    if data_type == "boolean":
        s = str(value).strip().lower()
        if s in ("true", "1", "yes", "y"):
            return "true"
        if s in ("false", "0", "no", "n"):
            return "false"
        return f"'{escape_cypher_value(value)}'"

    return f"'{escape_cypher_value(value)}'"
