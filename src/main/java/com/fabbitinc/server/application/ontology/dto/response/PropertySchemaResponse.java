package com.fabbitinc.server.application.ontology.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;

@Schema(description = "응답 DTO")
public record PropertySchemaResponse(
        String name,
        String description,
        PropertyDataType dataType,
        boolean required,
        boolean isMergeKey
) {
}
