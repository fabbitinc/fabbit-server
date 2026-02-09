"""하위호환용 re-export.

base_ontology.py로 이전되었습니다.
기존 코드에서 constants를 참조하는 경우를 위해 유지합니다.
"""

from app.modules.ontology.base_ontology import (  # noqa: F401
    MANUFACTURING_ONTOLOGY,
    BaseOntology,
    NodeLabel,
    PropertyDef,
    RelationshipType,
)
