package com.fabbitinc.server.application.ontology.dto.response;

import java.util.List;

public record OntologySchemaResponse(
        String name,
        String description,
        List<NodeLabelSchemaResponse> nodeLabels,
        List<RelationshipTypeSchemaResponse> relationshipTypes
) {
}
