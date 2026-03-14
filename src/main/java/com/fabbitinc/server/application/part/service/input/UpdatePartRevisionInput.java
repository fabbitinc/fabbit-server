package com.fabbitinc.server.application.part.service.input;

import java.util.LinkedHashMap;
import java.util.Map;

public record UpdatePartRevisionInput(
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String name,
        boolean nameSet,
        String material,
        boolean materialSet,
        String unit,
        boolean unitSet,
        String description,
        boolean descriptionSet,
        String category,
        boolean categorySet,
        Boolean phantom,
        boolean phantomSet,
        Integer leadTimeDays,
        boolean leadTimeDaysSet,
        Map<String, Object> extendedProperties,
        boolean extendedPropertiesSet
) {
    public UpdatePartRevisionInput {
        extendedProperties = extendedProperties == null ? null : new LinkedHashMap<>(extendedProperties);
    }

    public boolean hasAnyFieldSet() {
        return nameSet
                || materialSet
                || unitSet
                || descriptionSet
                || categorySet
                || phantomSet
                || leadTimeDaysSet
                || extendedPropertiesSet;
    }
}
