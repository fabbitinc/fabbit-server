package com.fabbitinc.server.application.ontology.dto.response;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "응답 DTO")
public record PropertySchemaResponse(
        String name,
        String description,
        PropertyDataType dataType,
        boolean required,
        boolean isMergeKey
) {
}
