package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record BomChildResponse(
        UUID id,
        String partNumber,
        String name,
        String revisionCode,
        String lineNumber,
        BigDecimal quantity,
        Map<String, Object> extendedProperties
) {
}
