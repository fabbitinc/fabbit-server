package com.fabbitinc.server.application.ontology.dto.response;

import java.util.List;

public record RelationshipTypeSchemaResponse(
        String relType,
        String description,
        String fromLabel,
        String toLabel,
        List<PropertySchemaResponse> properties
) {
}
