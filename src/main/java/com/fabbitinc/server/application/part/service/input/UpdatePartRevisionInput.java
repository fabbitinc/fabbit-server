package com.fabbitinc.server.application.part.service.input;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record UpdatePartRevisionInput(
        UUID partId,
        UUID revisionId,
        String name,
        boolean nameSet,
        String material,
        boolean materialSet,
        String unit,
        boolean unitSet,
        String description,
        boolean descriptionSet,
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
                || leadTimeDaysSet
                || extendedPropertiesSet;
    }
}
