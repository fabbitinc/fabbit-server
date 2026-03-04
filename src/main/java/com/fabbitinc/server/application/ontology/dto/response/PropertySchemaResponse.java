package com.fabbitinc.server.application.ontology.dto.response;

public record PropertySchemaResponse(
        String name,
        String description,
        String dataType,
        boolean required,
        boolean isMergeKey
) {
}
