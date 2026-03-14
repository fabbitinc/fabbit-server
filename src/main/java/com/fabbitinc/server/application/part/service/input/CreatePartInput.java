package com.fabbitinc.server.application.part.service.input;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
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
        PartLifecycleState lifecycleState,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties,
        String reason
) {
    public CreatePartInput {
        extendedProperties = extendedProperties == null ? Map.of() : new LinkedHashMap<>(extendedProperties);
    }
}
