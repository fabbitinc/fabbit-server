"""온톨로지 스키마 조회."""

from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import (
    NodeLabelSchema,
    OntologySchemaResponse,
    PropertySchema,
    RelationshipTypeSchema,
)

_cached_schema: OntologySchemaResponse | None = None


def get_ontology_schema() -> OntologySchemaResponse:
    """온톨로지 스키마 조회 (정적 데이터, 1회 빌드 후 캐싱)."""
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
