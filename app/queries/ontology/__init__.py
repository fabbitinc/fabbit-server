"""Ontology 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.ontology.get_schema import get_ontology_schema
from app.queries.ontology.search_nodes import search_nodes

__all__ = [
    "get_ontology_schema",
    "search_nodes",
]
