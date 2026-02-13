"""온톨로지 스키마 API — 정적 스키마 조회."""

from fastapi import APIRouter
from pydantic import BaseModel

from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY

router = APIRouter(prefix="/api/v1/ontology", tags=["ontology"])


# === 응답 스키마 ===


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


# === 스키마 빌드 (앱 시작 시 1회) ===

_cached_schema: OntologySchemaResponse | None = None


def _build_schema() -> OntologySchemaResponse:
    global _cached_schema
    if _cached_schema is not None:
        return _cached_schema

    ont = MANUFACTURING_ONTOLOGY

    node_labels = []
    for nl in ont.node_labels:
        props = [
            PropertySchema(
                name=p.name,
                description=p.description,
                data_type=p.data_type,
                required=p.required,
                is_merge_key=p.is_merge_key,
            )
            for p in nl.properties
        ]
        node_labels.append(
            NodeLabelSchema(
                label=nl.label,
                description=nl.description,
                properties=props,
                merge_keys=nl.merge_keys,
            )
        )

    relationship_types = []
    for rt in ont.relationship_types:
        props = [
            PropertySchema(
                name=p.name,
                description=p.description,
                data_type=p.data_type,
                required=p.required,
                is_merge_key=p.is_merge_key,
            )
            for p in rt.properties
        ]
        relationship_types.append(
            RelationshipTypeSchema(
                rel_type=rt.rel_type,
                description=rt.description,
                from_label=rt.from_label,
                to_label=rt.to_label,
                properties=props,
            )
        )

    _cached_schema = OntologySchemaResponse(
        name=ont.name,
        description=ont.description,
        node_labels=node_labels,
        relationship_types=relationship_types,
    )
    return _cached_schema


# === 엔드포인트 ===


@router.get("/schema", response_model=OntologySchemaResponse)
def get_ontology_schema():
    """온톨로지 스키마 조회 (정적 데이터, 인증 불필요)."""
    return _build_schema()
