package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record BomParentResponse(
        UUID id,
        String partNumber,
        String name,
        String revisionCode,
        String lineNumber,
        BigDecimal quantity,
        Map<String, Object> extendedProperties
) {
}
