package com.fabbitinc.server.application.ontology.dto.response;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;

public record PropertySchemaResponse(
        String name,
        String description,
        PropertyDataType dataType,
        boolean required,
        boolean isMergeKey
) {
}
