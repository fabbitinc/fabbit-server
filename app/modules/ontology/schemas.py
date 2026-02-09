"""Pydantic 요청/응답 모델.

파이프라인 API에서 사용하는 매핑, 인제스션, 질의 관련 스키마입니다.
"""

from pydantic import BaseModel


# === 매핑 관련 ===

class ColumnMapping(BaseModel):
    """Excel 컬럼 → 온톨로지 노드 속성 매핑"""
    source_column: str
    target_label: str
    target_property: str
    data_type: str = "string"  # string | integer | float | boolean


class RelationMapping(BaseModel):
    """노드 간 관계 매핑"""
    from_label: str
    to_label: str
    rel_type: str
    properties: dict[str, str] = {}  # source_column → rel_property
    property_types: dict[str, str] = {}  # rel_property → data_type


class ExtendedPropertyMapping(BaseModel):
    """확장 속성 매핑 (온톨로지에 정의되지 않은 추가 컬럼)"""
    source_column: str
    target_label: str
    property_name: str  # _ext_ 프리픽스가 붙은 속성명
    data_type: str = "string"  # string | integer | float | boolean


class MappingResult(BaseModel):
    """LLM 매핑 결과 전체"""
    column_mappings: list[ColumnMapping]
    relation_mappings: list[RelationMapping]
    extended_properties: list[ExtendedPropertyMapping]


# === API 요청/응답 ===

class MappingConfirmRequest(BaseModel):
    """매핑 확정 요청 - 사용자가 검토 후 확인"""
    org_id: str
    name: str
    original_headers: list[str]
    mapping: MappingResult


class QueryRequest(BaseModel):
    """자연어 질의 요청"""
    org_id: str
    question: str


class CypherRequest(BaseModel):
    """자연어 → Cypher 변환 요청"""
    question: str


class IngestionStats(BaseModel):
    """인제스션 결과 통계"""
    total_rows: int
    nodes_created: int
    relationships_created: int
    errors: list[str] = []


class QueryResponse(BaseModel):
    """질의 응답"""
    cypher_query: str
    results: list[dict]
