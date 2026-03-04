package com.fabbitinc.server.application.part.dto.response;

import java.util.Map;
import java.util.UUID;

public record BomChildResponse(
        UUID id,
        String partNumber,
        String name,
        int quantity,
        Map<String, Object> extendedProperties
) {
}
