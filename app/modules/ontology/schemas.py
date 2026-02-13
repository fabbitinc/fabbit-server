"""온톨로지 매핑 스키마.

LLM 매핑 결과를 표현하는 Pydantic 모델입니다.
"""

from pydantic import BaseModel


# === 매핑 관련 ===

class ColumnMapping(BaseModel):
    """Excel 컬럼 → 온톨로지 노드 속성 매핑"""
    source_column: str
    target_label: str
    target_property: str
    data_type: str = "string"  # string | integer | float | boolean
    confidence: int = 0  # 0-100, LLM 매핑 신뢰도
    reason: str = ""  # 매핑 근거 (1줄)


class RelationMapping(BaseModel):
    """노드 간 관계 매핑"""
    from_label: str
    to_label: str
    rel_type: str
    # 엔드포인트별 merge key → source_column 매핑
    # CONSISTS_OF 등 from/to가 같은 라벨일 때 필수
    # 예: {"part_number": "상위품번"}, {"part_number": "하위품번"}
    from_columns: dict[str, str] = {}  # merge_key → source_column
    to_columns: dict[str, str] = {}    # merge_key → source_column
    properties: dict[str, str] = {}  # source_column → rel_property
    property_types: dict[str, str] = {}  # rel_property → data_type


class ExtendedPropertyMapping(BaseModel):
    """확장 속성 매핑 (온톨로지에 정의되지 않은 추가 컬럼)"""
    source_column: str
    target_label: str
    property_name: str  # _ext_ 프리픽스가 붙은 속성명
    data_type: str = "string"  # string | integer | float | boolean
    confidence: int = 0  # 0-100, LLM 매핑 신뢰도
    reason: str = ""  # 매핑 근거 (1줄)


class MappingResult(BaseModel):
    """LLM 매핑 결과 전체"""
    column_mappings: list[ColumnMapping]
    relation_mappings: list[RelationMapping]
    extended_properties: list[ExtendedPropertyMapping]
