package com.fabbitinc.server.application.ontology.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record OntologySchemaResponse(
        String name,
        String description,
        List<NodeLabelSchemaResponse> nodeLabels,
        List<RelationshipTypeSchemaResponse> relationshipTypes
) {
}
