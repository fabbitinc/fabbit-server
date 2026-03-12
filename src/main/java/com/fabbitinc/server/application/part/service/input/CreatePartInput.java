package com.fabbitinc.server.application.part.service.input;

import java.util.LinkedHashMap;
import java.util.Map;

public record CreatePartInput(
        String partNumber,
        String name,
        String material,
        String unit,
        String description,
        String category,
        Boolean phantom,
        String lifecycleState,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties
) {
    public CreatePartInput {
        extendedProperties = extendedProperties == null ? Map.of() : new LinkedHashMap<>(extendedProperties);
    }
}
