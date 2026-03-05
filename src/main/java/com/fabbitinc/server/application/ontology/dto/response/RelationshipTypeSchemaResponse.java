package com.fabbitinc.server.application.ontology.dto.response;

import com.fabbitinc.server.application.ontology.support.RelationshipType;

import java.util.List;

public record RelationshipTypeSchemaResponse(
        RelationshipType relType,
        String description,
        String fromLabel,
        String toLabel,
        List<PropertySchemaResponse> properties
) {
}
