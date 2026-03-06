package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record BomParentResponse(
        UUID id,
        String partNumber,
        String name,
        int quantity,
        Map<String, Object> extendedProperties
) {
}
