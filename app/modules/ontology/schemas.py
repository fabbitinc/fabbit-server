"""온톨로지 매핑 스키마.

LLM 매핑 결과를 표현하는 Pydantic 모델입니다.

매핑 구조 (Part 속성 / 외부 관계 이분법):
  - PropertyMapping: 행의 주인공 Part에 귀속되는 속성 매핑
  - RelationMapping: 외부 노드와의 관계 매핑 (상위 Part, Supplier, Drawing 등)
"""

from pydantic import BaseModel


# === 매핑 관련 ===


class PropertyMapping(BaseModel):
    """Part 속성 매핑 — 행의 주인공 Part에 귀속.

    온톨로지 정의 속성과 확장 속성(_ext_) 모두 포함.
    """

    source_column: str
    target_property: str  # Part 속성명 (예: part_number, name, _ext_weight)
    data_type: str = "string"  # string | integer | float | boolean
    confidence: int = 0  # 0-100, LLM 매핑 신뢰도
    reason: str = ""  # 매핑 근거 (1줄)
    is_extended: bool = False  # 확장 속성 여부 (표시명은 source_column 사용)


class RelationMapping(BaseModel):
    """외부 관계 매핑 — 관계 + 상대방 노드.

    CONSISTS_OF: 상위 Part (from) → 주 Part (to) 방향.
      node_columns은 상위 Part(from)의 속성을 매핑.
    SUPPLIED_BY, DEFINED_BY: 주 Part (from) → 외부 노드 (to) 방향.
      node_columns은 외부 노드(to)의 속성을 매핑.
    """

    rel_type: str  # CONSISTS_OF, SUPPLIED_BY, DEFINED_BY, HAS_ITEM
    target_label: str  # 상대방 노드 라벨 (Part, Supplier, Drawing)
    # 상대방 노드 속성: {property_name: source_column}
    node_columns: dict[str, str]  # {"part_number": "상위품번", "name": "상위품명"}
    # 관계 속성: {property_name: source_column}
    rel_columns: dict[str, str] = {}  # {"quantity": "수량"}
    # 관계 속성 타입: {property_name: data_type}
    rel_column_types: dict[str, str] = {}  # {"quantity": "integer"}
    confidence: int = 0  # 0-100, LLM 매핑 신뢰도
    reason: str = ""  # 매핑 근거 (1줄)


class MappingResult(BaseModel):
    """LLM 매핑 결과 전체"""

    property_mappings: list[PropertyMapping]
    relation_mappings: list[RelationMapping]

    def get_required_columns(self) -> list[str]:
        """매핑에 실제 사용된 소스 컬럼 목록 (합성 검증 기준)."""
        seen: set[str] = set()
        result: list[str] = []
        for pm in self.property_mappings:
            if pm.source_column not in seen:
                seen.add(pm.source_column)
                result.append(pm.source_column)
        for rm in self.relation_mappings:
            for col in rm.node_columns.values():
                if col not in seen:
                    seen.add(col)
                    result.append(col)
            for col in rm.rel_columns.values():
                if col not in seen:
                    seen.add(col)
                    result.append(col)
        return result


# === 온톨로지 스키마 조회 ===


class PropertySchema(BaseModel):
    name: str
    description: str
    data_type: str
    required: bool
    is_merge_key: bool


class NodeLabelSchema(BaseModel):
    label: str
    description: str
    properties: list[PropertySchema]
    merge_keys: list[str]


class RelationshipTypeSchema(BaseModel):
    rel_type: str
    description: str
    from_label: str
    to_label: str
    properties: list[PropertySchema]


class OntologySchemaResponse(BaseModel):
    name: str
    description: str
    node_labels: list[NodeLabelSchema]
    relationship_types: list[RelationshipTypeSchema]
