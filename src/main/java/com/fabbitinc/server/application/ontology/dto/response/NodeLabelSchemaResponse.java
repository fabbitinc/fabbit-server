package com.fabbitinc.server.application.ontology.dto.response;

import java.util.List;

public record NodeLabelSchemaResponse(
        String label,
        String description,
        List<PropertySchemaResponse> properties,
        List<String> mergeKeys
) {
}
