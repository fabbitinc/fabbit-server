package com.fabbitinc.server.application.ontology.query.condition;

public record NodeSearchCondition(
        String label,
        String search,
        int limit
) {
}
