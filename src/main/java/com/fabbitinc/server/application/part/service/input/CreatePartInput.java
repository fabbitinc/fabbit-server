package com.fabbitinc.server.application.part.service.input;

import com.fabbitinc.server.domain.part.model.PartItemType;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CreatePartInput(
        String partNumber,
        UUID categoryId,
        PartItemType itemType,
        String name,
        String material,
        String unit,
        String description,
        PartLifecycleState lifecycleState,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties,
        String reason
) {
    public CreatePartInput {
        extendedProperties = extendedProperties == null ? Map.of() : new LinkedHashMap<>(extendedProperties);
    }
}
